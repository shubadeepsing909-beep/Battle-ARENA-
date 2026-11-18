const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "kOXOAgPVKudCdefAI8x22hD32TQ2";
const ADMIN_EMAIL = "shubadeepsing909@gmail.com";
const PLAYER_A_UID = "player_a_101";
const PLAYER_B_UID = "player_b_202";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function adminContext() {
  return testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  });
}

test("Unauthenticated user: denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("tournaments").get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(unauthDb.collection("wallets").doc(PLAYER_A_UID).get());
  await assertFails(unauthDb.collection("deposits").get());
  await assertFails(unauthDb.collection("auditLogs").get());
});

test("Email spoofing: unverified admin email cannot act as admin", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("wallets").doc(PLAYER_A_UID).set({
      userId: PLAYER_A_UID,
      balance: 99999,
      totalDeposited: 99999,
      totalWithdrawn: 0,
      totalWinnings: 0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Wallet protection: player cannot create or tamper with own wallet balance (₹99,999 attack blocked)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("wallets").doc(PLAYER_A_UID).set({
      userId: PLAYER_A_UID,
      balance: 100,
      totalDeposited: 100,
      totalWithdrawn: 0,
      totalWinnings: 0,
      updatedAt: new Date(Date.now() - 5000),
    });
  });

  const playerDb = testEnv.authenticatedContext(PLAYER_A_UID).firestore();
  await assertSucceeds(playerDb.collection("wallets").doc(PLAYER_A_UID).get());
  await assertFails(
    playerDb.collection("wallets").doc(PLAYER_A_UID).update({
      balance: 99999,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    playerDb.collection("wallets").doc(PLAYER_B_UID).get()
  );
});

test("Admin: can create/update tournaments, approve pending deposits once, and block duplicate approval", async () => {
  const adminDb = adminContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    adminDb.collection("tournaments").doc("tour_1").set({
      title: "Bermuda Pro Cup",
      gameMode: "SQUAD",
      mapName: "Bermuda",
      entryFee: 50,
      prizePool: 2000,
      firstPrize: 1200,
      secondPrize: 500,
      thirdPrize: 300,
      perKillPrize: 10,
      maxPlayers: 48,
      registeredCount: 0,
      scheduledDate: "2026-11-20",
      startTime: "20:00",
      registrationCloseTime: "19:45",
      rules: "No emulators. Fair play.",
      status: "UPCOMING",
      published: true,
      roomId: "",
      roomPassword: "",
      createdBy: ADMIN_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player submits pending deposit
  const playerDb = testEnv.authenticatedContext(PLAYER_A_UID).firestore();
  await assertSucceeds(
    playerDb.collection("deposits").doc("dep_1").set({
      userId: PLAYER_A_UID,
      playerName: "Aarav",
      amount: 100,
      paymentMethod: "UPI",
      paymentLinkUsed: "https://www.lg-pay.com/order/upi/ORDCEQIG0Z5EU",
      transactionRef: "UTR123456789",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Player cannot approve their own deposit
  await assertFails(
    playerDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Admin approves pending deposit
  await assertSucceeds(
    adminDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      adminNote: "Verified UTR",
      reviewedBy: ADMIN_UID,
      reviewedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Duplicate approval blocked by terminal state lock
  await assertFails(
    adminDb.collection("deposits").doc("dep_1").update({
      status: "APPROVED",
      adminNote: "Duplicate attempt",
      reviewedBy: ADMIN_UID,
      reviewedAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Admin: bank withdrawal approval & immutable audit logs", async () => {
  const adminDb = adminContext().firestore();
  const playerDb = testEnv.authenticatedContext(PLAYER_A_UID).firestore();
  const now = new Date(Date.now() - 3000);

  await assertSucceeds(
    playerDb.collection("withdrawals").doc("wd_1").set({
      userId: PLAYER_A_UID,
      playerName: "Aarav",
      amount: 200,
      payoutMethod: "BANK_ACCOUNT",
      accountHolderName: "Aarav Sharma",
      accountNumber: "123456789012",
      ifscCode: "SBIN0001234",
      bankName: "State Bank of India",
      status: "PENDING",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    adminDb.collection("withdrawals").doc("wd_1").update({
      status: "APPROVED",
      payoutReference: "IMPS99887766",
      adminNote: "Bank transfer sent",
      reviewedBy: ADMIN_UID,
      reviewedAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    adminDb.collection("auditLogs").doc("log_1").set({
      adminUid: ADMIN_UID,
      adminEmail: ADMIN_EMAIL,
      action: "APPROVE_WITHDRAWAL",
      targetType: "WITHDRAWAL",
      targetId: "wd_1",
      details: "Approved bank payout ₹200 Ref IMPS99887766",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Audit logs are immutable
  await assertFails(
    adminDb.collection("auditLogs").doc("log_1").update({
      details: "Tampered",
    })
  );
});
