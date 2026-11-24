package com.example.data

import com.example.data.model.FieldType
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.TemplateField

object DefaultTemplates {
    const val DEFAULT_HOSPITAL_HEADER = "Grant Government Medical College & Sir JJ Group of Hospitals, Mumbai"

    fun builtInTemplates(): List<FormTemplate> = listOf(
        lftTemplate(),
        rftTemplate(),
        cbcTemplate(),
        surgerySpecimenTemplate(),
        hmisDischargeCardTemplate()
    )

    private fun lftTemplate() = FormTemplate(
        templateKey = "lft",
        name = "LFT Requisition & Report",
        category = FormCategory.LAB.name,
        hospitalHeader = DEFAULT_HOSPITAL_HEADER,
        departmentSubtitle = "Department of Clinical Biochemistry — Liver Function Test",
        description = "Standard Liver Function Test panel with bilirubin, transaminases, proteins, and clinical indication.",
        footerText = "Sample Collected By: ________________   Resident / Pathologist Sign: ________________",
        builtIn = true,
        fields = listOf(
            TemplateField("urgency", "Priority / Urgency", FieldType.TEXT.name, "", "Routine", "Requisition Details", ""),
            TemplateField("sample_type", "Sample Type", FieldType.TEXT.name, "", "Serum (Plain / Clot Tube)", "Requisition Details", ""),
            TemplateField("clinical_indication", "Clinical Indication / Diagnosis", FieldType.TEXT.name, "", "", "Requisition Details", "diagnosis"),
            TemplateField("total_bilirubin", "Total Bilirubin", FieldType.NUMBER.name, "mg/dL (0.2 - 1.2)", "", "Bilirubin Profile", ""),
            TemplateField("direct_bilirubin", "Direct (Conjugated) Bilirubin", FieldType.NUMBER.name, "mg/dL (0.0 - 0.3)", "", "Bilirubin Profile", ""),
            TemplateField("indirect_bilirubin", "Indirect Bilirubin", FieldType.NUMBER.name, "mg/dL (0.1 - 0.9)", "", "Bilirubin Profile", ""),
            TemplateField("sgot_ast", "SGOT (AST)", FieldType.NUMBER.name, "U/L (5 - 40)", "", "Hepatic Enzymes", ""),
            TemplateField("sgpt_alt", "SGPT (ALT)", FieldType.NUMBER.name, "U/L (7 - 56)", "", "Hepatic Enzymes", ""),
            TemplateField("alk_phos", "Alkaline Phosphatase (ALP)", FieldType.NUMBER.name, "U/L (44 - 147)", "", "Hepatic Enzymes", ""),
            TemplateField("ggt", "GGT", FieldType.NUMBER.name, "U/L (9 - 48)", "", "Hepatic Enzymes", ""),
            TemplateField("total_protein", "Total Protein", FieldType.NUMBER.name, "g/dL (6.4 - 8.3)", "", "Serum Proteins", ""),
            TemplateField("serum_albumin", "Serum Albumin", FieldType.NUMBER.name, "g/dL (3.5 - 5.0)", "", "Serum Proteins", ""),
            TemplateField("serum_globulin", "Serum Globulin", FieldType.NUMBER.name, "g/dL (2.0 - 3.5)", "", "Serum Proteins", ""),
            TemplateField("ag_ratio", "A/G Ratio", FieldType.TEXT.name, "Ratio (1.1 - 2.2)", "", "Serum Proteins", "")
        )
    )

    private fun rftTemplate() = FormTemplate(
        templateKey = "rft",
        name = "RFT & Electrolytes Form",
        category = FormCategory.LAB.name,
        hospitalHeader = DEFAULT_HOSPITAL_HEADER,
        departmentSubtitle = "Department of Clinical Biochemistry — Renal Function Test",
        description = "Renal Function Test & Serum Electrolytes requisition and result entry form.",
        footerText = "Sample Collected By: ________________   Biochemist / Resident Sign: ________________",
        builtIn = true,
        fields = listOf(
            TemplateField("urgency", "Priority / Urgency", FieldType.TEXT.name, "", "Routine", "Requisition Details", ""),
            TemplateField("clinical_indication", "Clinical Indication", FieldType.TEXT.name, "", "", "Requisition Details", "diagnosis"),
            TemplateField("blood_urea", "Blood Urea / BUN", FieldType.NUMBER.name, "mg/dL (15 - 45)", "", "Renal Parameters", ""),
            TemplateField("serum_creatinine", "Serum Creatinine", FieldType.NUMBER.name, "mg/dL (0.6 - 1.3)", "", "Renal Parameters", ""),
            TemplateField("uric_acid", "Serum Uric Acid", FieldType.NUMBER.name, "mg/dL (3.5 - 7.2)", "", "Renal Parameters", ""),
            TemplateField("egfr", "Estimated GFR (eGFR)", FieldType.NUMBER.name, "mL/min/1.73m2 (>90)", "", "Renal Parameters", ""),
            TemplateField("sodium", "Serum Sodium (Na+)", FieldType.NUMBER.name, "mEq/L (135 - 145)", "", "Serum Electrolytes", ""),
            TemplateField("potassium", "Serum Potassium (K+)", FieldType.NUMBER.name, "mEq/L (3.5 - 5.1)", "", "Serum Electrolytes", ""),
            TemplateField("chloride", "Serum Chloride (Cl-)", FieldType.NUMBER.name, "mEq/L (96 - 106)", "", "Serum Electrolytes", ""),
            TemplateField("calcium", "Serum Calcium (Total)", FieldType.NUMBER.name, "mg/dL (8.5 - 10.5)", "", "Serum Electrolytes", ""),
            TemplateField("phosphorus", "Serum Phosphorus", FieldType.NUMBER.name, "mg/dL (2.5 - 4.5)", "", "Serum Electrolytes", ""),
            TemplateField("urine_output", "24h Urine Output / Remarks", FieldType.TEXT.name, "mL / 24h", "", "Clinical Remarks", "")
        )
    )

    private fun cbcTemplate() = FormTemplate(
        templateKey = "cbc",
        name = "CBC Hematology Form",
        category = FormCategory.LAB.name,
        hospitalHeader = DEFAULT_HOSPITAL_HEADER,
        departmentSubtitle = "Central Clinical Pathology Lab — Complete Blood Count",
        description = "Complete Blood Count with differential counts, RBC indices, platelets, and peripheral smear.",
        footerText = "EDTA Sample Verified By: ________________   Hematologist / Resident Sign: ________________",
        builtIn = true,
        fields = listOf(
            TemplateField("sample_type", "Sample Tube", FieldType.TEXT.name, "", "EDTA Whole Blood (Lavender)", "Requisition Details", ""),
            TemplateField("hemoglobin", "Hemoglobin (Hb)", FieldType.NUMBER.name, "g/dL (M:13-17, F:12-15)", "", "Red Cell & Platelets", ""),
            TemplateField("total_wbc", "Total WBC Count (TLC)", FieldType.NUMBER.name, "/cumm (4,000 - 11,000)", "", "Red Cell & Platelets", ""),
            TemplateField("rbc_count", "RBC Count", FieldType.NUMBER.name, "mill/cumm (4.5 - 5.5)", "", "Red Cell & Platelets", ""),
            TemplateField("platelet_count", "Platelet Count", FieldType.NUMBER.name, "/cumm (1.5 - 4.5 Lakh)", "", "Red Cell & Platelets", ""),
            TemplateField("pcv_hct", "PCV / Hematocrit", FieldType.NUMBER.name, "% (36 - 48)", "", "RBC Indices", ""),
            TemplateField("mcv", "MCV", FieldType.NUMBER.name, "fL (80 - 100)", "", "RBC Indices", ""),
            TemplateField("mch", "MCH", FieldType.NUMBER.name, "pg (27 - 32)", "", "RBC Indices", ""),
            TemplateField("mchc", "MCHC", FieldType.NUMBER.name, "g/dL (32 - 36)", "", "RBC Indices", ""),
            TemplateField("neutrophils", "Neutrophils", FieldType.NUMBER.name, "% (40 - 75)", "", "Differential Count (DLC)", ""),
            TemplateField("lymphocytes", "Lymphocytes", FieldType.NUMBER.name, "% (20 - 45)", "", "Differential Count (DLC)", ""),
            TemplateField("monocytes", "Monocytes", FieldType.NUMBER.name, "% (2 - 10)", "", "Differential Count (DLC)", ""),
            TemplateField("eosinophils", "Eosinophils", FieldType.NUMBER.name, "% (1 - 6)", "", "Differential Count (DLC)", ""),
            TemplateField("basophils", "Basophils", FieldType.NUMBER.name, "% (0 - 1)", "", "Differential Count (DLC)", ""),
            TemplateField("esr", "ESR (1st Hour)", FieldType.NUMBER.name, "mm/hr (0 - 20)", "", "Smear & ESR", ""),
            TemplateField("peripheral_smear", "Peripheral Blood Smear", FieldType.MULTILINE.name, "", "", "Smear & ESR", "")
        )
    )

    private fun surgerySpecimenTemplate() = FormTemplate(
        templateKey = "surgery_specimen",
        name = "Surgery Specimen / Histopathology Form",
        category = FormCategory.SURGERY.name,
        hospitalHeader = DEFAULT_HOSPITAL_HEADER,
        departmentSubtitle = "Department of Pathology — Histopathology & Surgical Specimen Requisition",
        description = "Surgical specimen submission form capturing OT procedure, site, fixative, containers, and intra-op findings.",
        footerText = "Received in Pathology Lab: ________________   Operating Surgeon Sign & Stamp: ________________",
        builtIn = true,
        fields = listOf(
            TemplateField("surgery_date", "Date & Time of Surgery", FieldType.DATE.name, "", "", "Operative Details", ""),
            TemplateField("ot_procedure", "Name of Operative Procedure", FieldType.TEXT.name, "", "", "Operative Details", ""),
            TemplateField("operating_surgeon", "Operating Surgeon", FieldType.TEXT.name, "", "", "Operative Details", "admittingDoctor"),
            TemplateField("clinical_diagnosis", "Clinical / Pre-op Diagnosis", FieldType.TEXT.name, "", "", "Operative Details", "diagnosis"),
            TemplateField("specimen_nature", "Nature & Anatomical Site of Specimen", FieldType.MULTILINE.name, "", "", "Specimen Details", ""),
            TemplateField("laterality", "Laterality / Orientation Markers", FieldType.TEXT.name, "Left / Right / Midline", "", "Specimen Details", ""),
            TemplateField("fixative", "Fixative Used", FieldType.TEXT.name, "", "10% Neutral Buffered Formalin", "Specimen Details", ""),
            TemplateField("container_count", "Number of Specimen Containers", FieldType.NUMBER.name, "Containers", "1", "Specimen Details", ""),
            TemplateField("urgent_frozen", "Urgent / Frozen Section Requested", FieldType.CHECKBOX.name, "", "No", "Specimen Details", ""),
            TemplateField("intraop_findings", "Intra-Operative & Gross Findings", FieldType.MULTILINE.name, "", "", "Clinical Summary", ""),
            TemplateField("clinical_history", "Brief Clinical History & Relevant Imaging", FieldType.MULTILINE.name, "", "", "Clinical Summary", "extraNotes"),
            TemplateField("sent_by", "Specimen Sent By (Name & Designation)", FieldType.TEXT.name, "", "", "Chain of Custody", "")
        )
    )

    private fun hmisDischargeCardTemplate() = FormTemplate(
        templateKey = "hmis_discharge",
        name = "HMIS Discharge Card",
        category = FormCategory.DISCHARGE.name,
        hospitalHeader = DEFAULT_HOSPITAL_HEADER,
        departmentSubtitle = "HMIS Inpatient Discharge Summary & Follow-Up Card",
        description = "Official HMIS inpatient discharge summary including diagnosis, hospital course, discharge Rx, and follow-up.",
        footerText = "Resident Doctor Sign: ________________   Unit Head / Consultant Sign: ________________   Patient / Relative Sign: ________________",
        builtIn = true,
        fields = listOf(
            TemplateField("admission_dt", "Date & Time of Admission", FieldType.DATE.name, "", "", "Admission & Discharge", "admissionDate"),
            TemplateField("discharge_dt", "Date & Time of Discharge", FieldType.DATE.name, "", "", "Admission & Discharge", ""),
            TemplateField("discharge_type", "Discharge Outcome / Type", FieldType.TEXT.name, "Improved / LAMA / DAMA / Referred", "Improved — Routine Discharge", "Admission & Discharge", ""),
            TemplateField("final_diagnosis", "Final Diagnosis (ICD / Clinical)", FieldType.MULTILINE.name, "", "", "Diagnosis & Complaints", "diagnosis"),
            TemplateField("chief_complaints", "Chief Complaints & History of Presenting Illness", FieldType.MULTILINE.name, "", "", "Diagnosis & Complaints", ""),
            TemplateField("examination_vitals", "Clinical Examination & Vitals on Admission", FieldType.MULTILINE.name, "", "", "Clinical Assessment", ""),
            TemplateField("investigations", "Key Investigations (CBC / LFT / RFT / Imaging)", FieldType.MULTILINE.name, "", "", "Clinical Assessment", ""),
            TemplateField("procedure_treatment", "Operative Procedure / Hospital Treatment Given", FieldType.MULTILINE.name, "", "", "Hospital Course", ""),
            TemplateField("course_in_ward", "Course in Hospital & Condition at Discharge", FieldType.MULTILINE.name, "", "", "Hospital Course", ""),
            TemplateField("discharge_meds", "Discharge Medications (Dose, Route, Frequency, Duration)", FieldType.MULTILINE.name, "", "", "Discharge Advice & Rx", ""),
            TemplateField("diet_activity", "Diet, Dressing & Activity Advice", FieldType.MULTILINE.name, "", "", "Discharge Advice & Rx", ""),
            TemplateField("followup_opd", "Follow-Up OPD Date, Room & Unit", FieldType.TEXT.name, "", "", "Follow-Up & Red Flags", ""),
            TemplateField("red_flags", "Emergency Warning Signs (Report to Casualty Immediately If)", FieldType.MULTILINE.name, "", "High-grade fever, severe pain, bleeding, vomiting, breathlessness, or wound discharge.", "Follow-Up & Red Flags", "")
        )
    )
}
