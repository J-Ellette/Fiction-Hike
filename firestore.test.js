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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated users are rejected from all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("community_treks").where("published", "==", true).get());
  await assertFails(unauthDb.collection("journey_logs").where("published", "==", true).get());
  await assertFails(unauthDb.collection("leaderboard_entries").where("activeOnBoard", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("Users can manage their own profile and active trek progress, isolated from others", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Strider",
      favoriteUniverse: "Lord of the Rings",
      totalDistanceKm: 12.5,
      treksCompleted: 1,
      loreDropsFound: 3,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(aliceDb.collection("users").get());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_progress").doc("lotr_frodo").set({
      trekId: "lotr_frodo",
      trekTitle: "Path to Mount Doom",
      universe: "Lord of the Rings",
      characterName: "Frodo Baggins",
      status: "active",
      distanceCoveredKm: 2.4,
      targetDistanceKm: 12.0,
      currentWaypointIndex: 2,
      unlockedLoreDropIds: ["lore_bag_end"],
      anchorLat: 37.7749,
      anchorLon: -122.4194,
      scaleKm: 12.0,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("active_progress").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("active_progress").get()
  );
});

test("Community treks support owner create/update, ghost-field rejection, and published queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  const validTrek = {
    creatorId: ALICE_UID,
    creatorName: "Strider",
    title: "Weathertop Ridge",
    universe: "Lord of the Rings",
    characterName: "Aragorn",
    description: "Stealth approach through the Chetwood to Amon Sul.",
    difficulty: "EPIC",
    estimatedKm: 8.5,
    elevationGainM: 420,
    accentColorHex: "#E8B84B",
    published: true,
    waypoints: [
      { dx: 0.0, dy: 0.0, elevM: 120.0, label: "Bree Gate" },
      { dx: 0.6, dy: 0.4, elevM: 540.0, label: "Amon Sul Summit" },
    ],
    loreDrops: [
      { id: "ld_1", title: "Blade of Elendil", category: "Quote", content: "Not all those who wander are lost.", waypointIndex: 1 },
    ],
    createdAt: past,
    updatedAt: past,
  };

  await assertSucceeds(aliceDb.collection("community_treks").doc("trek_custom_1").set(validTrek));

  await assertFails(
    aliceDb.collection("community_treks").doc("trek_custom_2").set({
      ...validTrek,
      id: "trek_custom_2",
    })
  );

  await assertSucceeds(bobDb.collection("community_treks").where("published", "==", true).get());
  await assertFails(bobDb.collection("community_treks").get());

  await assertFails(
    bobDb.collection("community_treks").doc("trek_custom_1").update({
      title: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Journey logs and leaderboard enforce ownership and query alignment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("journey_logs").doc("log_1").set({
      userId: ALICE_UID,
      authorName: "Strider",
      trekId: "got_arya",
      trekTitle: "King's Road North",
      universe: "Game of Thrones",
      characterName: "Arya Stark",
      distanceKm: 4.8,
      elevationGainM: 185,
      loreDropsUnlocked: 2,
      note: "Followed the river ridge before dusk.",
      moodTag: "Vigilant",
      published: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(bobDb.collection("journey_logs").where("published", "==", true).get());
  await assertSucceeds(aliceDb.collection("journey_logs").where("userId", "==", ALICE_UID).get());
  await assertFails(bobDb.collection("journey_logs").get());

  await assertSucceeds(
    aliceDb.collection("leaderboard_entries").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Strider",
      rankTitle: "Ranger of the North",
      activeTrekTitle: "King's Road North",
      activeCharacter: "Arya Stark",
      totalDistanceKm: 17.3,
      treksCompleted: 2,
      loreDropsFound: 5,
      activeOnBoard: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(bobDb.collection("leaderboard_entries").where("activeOnBoard", "==", true).get());
  await assertFails(bobDb.collection("leaderboard_entries").get());
  await assertFails(
    bobDb.collection("leaderboard_entries").doc(ALICE_UID).update({
      totalDistanceKm: 9999.0,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
