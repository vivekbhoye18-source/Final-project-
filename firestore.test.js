const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "clinician_alice_001";
const BOB_UID = "clinician_bob_002";

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

function samplePatient(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    mrdNo: "20260173023",
    fullName: "Mr. JALIL IBARAT ALI SHAIKH",
    gender: "Male",
    age: "40Y 0M 1D",
    ipdId: "260007625",
    department: "General Surgery",
    unit: "UNIT 6",
    wardBed: "EMERG WARD 4",
    admittingDoctor: "Dr. Waqar Ahmed Ansari",
    admissionDate: "15/11/2026 03:10 AM",
    mobile: "7738854284",
    address: "EKTA HOUSING SOC THANE WEST",
    mlcNumber: "EPR 20262/SJP",
    diagnosis: "Acute Abdomen",
    attendantName: "SHABANA SHAIKH",
    attendantRelation: "Sister",
    attendantMobile: "7738854284",
    extraNotes: "Shift pt to SICU",
    ocrConfidence: 94,
    createdAt: now,
    updatedAt: now,
  };
}

function sampleTemplate(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    templateKey: "lft",
    name: "LFT Requisition & Report",
    category: "LAB",
    hospitalHeader: "Sir JJ Group Of Hospitals, Mumbai",
    departmentSubtitle: "Department of Clinical Biochemistry",
    description: "Liver Function Test panel",
    footerText: "Verified by Resident / Pathologist",
    builtIn: true,
    fields: [
      {
        key: "total_bilirubin",
        label: "Total Bilirubin",
        fieldType: "NUMBER",
        unit: "mg/dL (0.2-1.2)",
        defaultValue: "",
        section: "Bilirubin Profile",
        patientMapping: "",
      },
    ],
    createdAt: now,
    updatedAt: now,
  };
}

function sampleFilledForm(uid, now = new Date(Date.now() - 2000)) {
  return {
    userId: uid,
    patientId: "patient_1",
    patientName: "Mr. JALIL IBARAT ALI SHAIKH",
    patientMrdNo: "20260173023",
    patientGender: "Male",
    patientAge: "40Y 0M 1D",
    patientWardBed: "EMERG WARD 4",
    patientDepartment: "General Surgery",
    patientDoctor: "Dr. Waqar Ahmed Ansari",
    patientIpdId: "260007625",
    patientAdmissionDate: "15/11/2026 03:10 AM",
    patientMlcNumber: "EPR 20262/SJP",
    templateId: "tpl_lft",
    templateName: "LFT Requisition & Report",
    templateCategory: "LAB",
    hospitalHeader: "Sir JJ Group Of Hospitals, Mumbai",
    departmentSubtitle: "Department of Clinical Biochemistry",
    footerText: "Verified by Resident / Pathologist",
    entries: [
      {
        key: "total_bilirubin",
        label: "Total Bilirubin",
        value: "1.1",
        unit: "mg/dL (0.2-1.2)",
        section: "Bilirubin Profile",
        fieldType: "NUMBER",
      },
    ],
    notes: "Fasting sample collected",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: denied read/write across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("patients").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("patients").doc("p1").set(samplePatient(ALICE_UID)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("templates").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("filledForms").get());
});

test("PatientRecord: owner create, list, partial update, delete succeed; invalid payloads fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const patientsCol = aliceDb.collection("users").doc(ALICE_UID).collection("patients");

  // Empty subcollection list succeeds
  await assertSucceeds(patientsCol.get());

  // Valid create
  await assertSucceeds(patientsCol.doc("patient_1").set(samplePatient(ALICE_UID)));
  await assertSucceeds(patientsCol.doc("patient_1").get());
  await assertSucceeds(patientsCol.get());

  // Valid partial update preserving createdAt & userId
  await assertSucceeds(
    patientsCol.doc("patient_1").update({
      wardBed: "SICU Bed 3",
      diagnosis: "Post-op Appendectomy",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field ("id" or unknown field)
  await assertFails(
    patientsCol.doc("patient_2").set({
      ...samplePatient(ALICE_UID),
      id: "patient_2",
    })
  );

  // Reject invalid gender enum
  await assertFails(
    patientsCol.doc("patient_3").set({
      ...samplePatient(ALICE_UID),
      gender: "INVALID_GENDER",
    })
  );

  // Reject future timestamp
  await assertFails(
    patientsCol.doc("patient_4").set({
      ...samplePatient(ALICE_UID),
      createdAt: new Date(Date.now() + 60000),
    })
  );

  // Reject createdAt mutation on update
  await assertFails(
    patientsCol.doc("patient_1").update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );

  // Reject cross-user access
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("patients").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("patients").doc("patient_1").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("patients").doc("patient_1").update({
      diagnosis: "Tampered",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Valid delete
  await assertSucceeds(patientsCol.doc("patient_1").delete());
});

test("FormTemplate: owner CRUD succeeds; ghost field & invalid category fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const tplCol = aliceDb.collection("users").doc(ALICE_UID).collection("templates");

  await assertSucceeds(tplCol.doc("tpl_lft").set(sampleTemplate(ALICE_UID)));
  await assertSucceeds(tplCol.get());

  await assertSucceeds(
    tplCol.doc("tpl_lft").update({
      name: "Updated LFT Panel",
      hospitalHeader: "GGMC & Sir JJ Hospital",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    tplCol.doc("tpl_bad").set({
      ...sampleTemplate(ALICE_UID),
      category: "INVALID_CAT",
    })
  );

  await assertFails(
    tplCol.doc("tpl_ghost").set({
      ...sampleTemplate(ALICE_UID),
      isAdmin: true,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("templates").doc("tpl_lft").get());
});

test("FilledFormRecord: owner CRUD succeeds; cross-user & immutable updates fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const formsCol = aliceDb.collection("users").doc(ALICE_UID).collection("filledForms");

  await assertSucceeds(formsCol.doc("form_1").set(sampleFilledForm(ALICE_UID)));
  await assertSucceeds(formsCol.get());

  await assertSucceeds(
    formsCol.doc("form_1").update({
      notes: "Printed & handed over to ward nurse",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    formsCol.doc("form_1").update({
      userId: BOB_UID,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("filledForms").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("filledForms").doc("form_1").delete());

  await assertSucceeds(formsCol.doc("form_1").delete());
});
