const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_999";
const ADMIN_EMAIL = "ayanrashid080@gmail.com";

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

function validSessionPayload(uid, email = "client@example.com") {
  const now = new Date(Date.now() - 1000);
  return {
    userId: uid,
    userEmail: email,
    activationKey: "NXS-VIP9-ARM64-8842",
    ipAddress: "203.0.113.42",
    hardwareId: "ARM64-NX-9F42-88A1",
    consentStatus: "ALLOWED",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user cannot read or write login_sessions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("login_sessions").get());
  await assertFails(
    unauthDb
      .collection("login_sessions")
      .doc("sess_1")
      .set(validSessionPayload(ALICE_UID))
  );
});

test("Authenticated client can create and query own consented login_session", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  await assertSucceeds(
    aliceDb
      .collection("login_sessions")
      .doc("sess_alice_1")
      .set(validSessionPayload(ALICE_UID, "alice@example.com"))
  );

  // Scoped query matching Kotlin LoginSessionRepository
  await assertSucceeds(
    aliceDb.collection("login_sessions").where("userId", "==", ALICE_UID).get()
  );

  // Unfiltered list query by non-admin must fail
  await assertFails(aliceDb.collection("login_sessions").get());
});

test("Rejects identity spoofing, ghost fields, future timestamps, and declined consent", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();

  // Identity spoofing
  await assertFails(
    aliceDb
      .collection("login_sessions")
      .doc("sess_spoof")
      .set(validSessionPayload(BOB_UID, "bob@example.com"))
  );

  // Ghost field / @DocumentId id leak
  await assertFails(
    aliceDb.collection("login_sessions").doc("sess_ghost").set({
      ...validSessionPayload(ALICE_UID),
      id: "sess_ghost",
    })
  );

  // Disallowed consentStatus
  await assertFails(
    aliceDb.collection("login_sessions").doc("sess_declined").set({
      ...validSessionPayload(ALICE_UID),
      consentStatus: "DECLINED",
    })
  );

  // Future timestamp
  const future = new Date(Date.now() + 60000);
  await assertFails(
    aliceDb.collection("login_sessions").doc("sess_future").set({
      ...validSessionPayload(ALICE_UID),
      createdAt: future,
      updatedAt: future,
    })
  );
});

test("Client cannot read another user's login_session, while verified admin can list all", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db
      .collection("login_sessions")
      .doc("sess_bob")
      .set(validSessionPayload(BOB_UID, "bob@example.com"));
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  await assertFails(aliceDb.collection("login_sessions").doc("sess_bob").get());

  // Spoofed unverified admin email must be denied
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(spoofAdminDb.collection("login_sessions").get());

  // Verified admin can list all consented login sessions
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("login_sessions").get());
  await assertSucceeds(adminDb.collection("login_sessions").doc("sess_bob").delete());
});
