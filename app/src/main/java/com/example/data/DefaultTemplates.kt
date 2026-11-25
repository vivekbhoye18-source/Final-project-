package com.example.data

import com.example.data.model.FieldType
import com.example.data.model.FormCategory
import com.example.data.model.FormTemplate
import com.example.data.model.TemplateField

object DefaultTemplates {
    const val DEFAULT_HOSPITAL_HEADER = "GRANT GOVT. MEDICAL COLLEGE & SIR J. J. GROUP OF HOSPITALS, MUMBAI"

    const val KEY_PATHOLOGY_REPORT = "ggmc_pathology_cbc"
    const val KEY_BIOCHEMISTRY_PROFILE = "ggmc_biochemistry_profile"
    const val KEY_BACTERIOLOGY_REQUEST = "ggmc_bacteriology_request"
    const val KEY_SEROLOGY_REQUEST = "ggmc_serology_request"
    const val KEY_HIV_CONSENT = "ggmc_hiv_consent"
    const val KEY_SURGERY_SPECIMEN = "surgery_specimen"
    const val KEY_HMIS_DISCHARGE = "hmis_discharge"

    val EXACT_HOSPITAL_LAYOUT_KEYS = setOf(
        KEY_PATHOLOGY_REPORT,
        KEY_BIOCHEMISTRY_PROFILE,
        KEY_BACTERIOLOGY_REQUEST,
        KEY_SEROLOGY_REQUEST,
        KEY_HIV_CONSENT
    )

    fun builtInTemplates(): List<FormTemplate> = listOf(
        pathologyReportTemplate(),
        biochemistryProfileTemplate(),
        bacteriologyRequestTemplate(),
        serologyRequestTemplate(),
        hivConsentTemplate(),
        surgerySpecimenTemplate(),
        hmisDischargeCardTemplate()
    )

    private fun pathologyReportTemplate() = FormTemplate(
        templateKey = KEY_PATHOLOGY_REPORT,
        name = "Pathology Report (CBC, Coag, Urine & Stool)",
        category = FormCategory.LAB.name,
        hospitalHeader = "GGMC AND SIR J. J. GROUP OF HOSPITALS, MUMBAI-08",
        departmentSubtitle = "DEPARTMENT OF PATHOLOGY",
        description = "Exact GGMC Pathology Report layout with CBC, Peripheral Smear, PSMP, ESR, Retic, Coagulation Profile, Urine & Stool Routine Microscopy.",
        footerText = "(G.C.P.) O 2663 (2,00,000-5-2025)",
        builtIn = true,
        fields = listOf(
            // Header fields
            TemplateField("sample_id", "SAMPLE ID", FieldType.TEXT.name, "", "", "Sample & Clinical Details", ""),
            TemplateField("clinical_details", "CLINICAL DETAILS", FieldType.TEXT.name, "", "", "Sample & Clinical Details", "diagnosis"),

            // CBC (17 rows)
            TemplateField("rbc_count", "RBC COUNT", FieldType.NUMBER.name, "X10^6/microL", "", "CBC", ""),
            TemplateField("haemoglobin", "HAEMOGLOBIN", FieldType.NUMBER.name, "g/dL", "", "CBC", ""),
            TemplateField("hematocrit", "HEMATOCRIT", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("mcv", "MCV", FieldType.NUMBER.name, "fL", "", "CBC", ""),
            TemplateField("mch", "MCH", FieldType.NUMBER.name, "Pg", "", "CBC", ""),
            TemplateField("mchc", "MCHC", FieldType.NUMBER.name, "g/dL", "", "CBC", ""),
            TemplateField("rdw_cv", "RDW-CV", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("wbc_count", "WBC COUNT", FieldType.NUMBER.name, "X10^3/microL", "", "CBC", ""),
            TemplateField("neutrophil", "NEUTROPHIL", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("lymphocyte", "LYMPHOCYTE", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("monocyte", "MONOCYTE", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("eosinophil", "EOSINOPHIL", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("basophil", "BASOPHIL", FieldType.NUMBER.name, "%", "", "CBC", ""),
            TemplateField("platelets", "PLATELETS", FieldType.NUMBER.name, "X10^3/microL", "", "CBC", ""),
            TemplateField("mpv", "MPV", FieldType.NUMBER.name, "fL", "", "CBC", ""),
            TemplateField("pdw", "PDW", FieldType.NUMBER.name, "fL", "", "CBC", ""),
            TemplateField("pct", "PCT", FieldType.NUMBER.name, "%", "", "CBC", ""),

            // Peripheral Smear & Coagulation Profile
            TemplateField("peripheral_smear", "PERIPHERAL SMEAR", FieldType.MULTILINE.name, "", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("psmp", "PSMP", FieldType.TEXT.name, "", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("esr", "ESR (mm/hr)", FieldType.NUMBER.name, "mm/hr", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("retic_count", "RETIC COUNT", FieldType.TEXT.name, "", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("pt_sec", "PT (sec)", FieldType.NUMBER.name, "sec", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("inr", "INR", FieldType.NUMBER.name, "", "", "Peripheral Smear & Coagulation Profile", ""),
            TemplateField("aptt_sec", "APTT (sec)", FieldType.NUMBER.name, "sec", "", "Peripheral Smear & Coagulation Profile", ""),

            // Urine Routine Microscopy
            TemplateField("urine_volume", "VOLUME", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_colour", "COLOUR", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_sugar", "SUGAR", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_protein", "PROTEIN", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_others_macro", "OTHERS (Physical/Chemical)", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_bacteria", "BACTERIA", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_parasite", "PARASITE", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_crystals", "CRYSTALS", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_cast", "CAST", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_rbc", "RBC", FieldType.TEXT.name, "/hpf", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_epithelial_cells", "EPITHELIAL CELLS", FieldType.TEXT.name, "/hpf", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_pus_cells", "PUS CELLS", FieldType.TEXT.name, "/hpf", "", "Urine Routine Microscopy", ""),
            TemplateField("urine_others_micro", "OTHERS (Microscopy)", FieldType.TEXT.name, "", "", "Urine Routine Microscopy", ""),

            // Stool Routine Microscopy
            TemplateField("stool_volume", "VOLUME", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_colour", "COLOUR", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_consistency", "CONSISTENCY", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_bacteria", "BACTERIA", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_parasite", "PARASITE", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_rbc", "RBC", FieldType.TEXT.name, "/hpf", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_pus_cells", "PUS CELLS", FieldType.TEXT.name, "/hpf", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_others", "OTHERS", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),
            TemplateField("stool_occult_blood", "OCCULT BLOOD", FieldType.TEXT.name, "", "", "Stool Routine Microscopy", ""),

            // Footer / Sign-off
            TemplateField("comment", "COMMENT", FieldType.MULTILINE.name, "", "", "Sign-Off", ""),
            TemplateField("case_seen_by", "CASE SEEN BY", FieldType.TEXT.name, "", "", "Sign-Off", ""),
            TemplateField("date_of_reporting", "DATE OF REPORTING", FieldType.DATE.name, "", "", "Sign-Off", "")
        )
    )

    private fun biochemistryProfileTemplate() = FormTemplate(
        templateKey = KEY_BIOCHEMISTRY_PROFILE,
        name = "Biochemistry Profile (LFT / RFT / Lipid / Diabetic)",
        category = FormCategory.LAB.name,
        hospitalHeader = "GRANT GOVT. MEDICAL COLLEGE & SIR J. J. GROUP OF HOSPITALS, MUMBAI",
        departmentSubtitle = "DEPARTMENT OF BIOCHEMISTRY",
        description = "Exact GGMC Biochemistry Profile with LFT/RFT (17 parameters), Lipid Profile (5 parameters), and Diabetic Profile (4 parameters).",
        footerText = "Please correlate clinically.",
        builtIn = true,
        fields = listOf(
            TemplateField("report_date", "Date", FieldType.DATE.name, "DD/MM/YYYY", "", "Header Details", ""),
            TemplateField("lab_no", "Lab. No.", FieldType.TEXT.name, "", "", "Header Details", ""),

            // LFT / RFT
            TemplateField("urea", "Urea", FieldType.NUMBER.name, "mg/dL | 15.0 – 40.0 mg/dL", "", "LFT / RFT", ""),
            TemplateField("creatinine", "Creatinine", FieldType.NUMBER.name, "mg/dL | 0.70 – 1.5 mg/dL", "", "LFT / RFT", ""),
            TemplateField("sgot_ast", "SGOT (AST)", FieldType.NUMBER.name, "IU/L | 8 – 40 IU/L", "", "LFT / RFT", ""),
            TemplateField("sgpt_alt", "SGPT (ALT)", FieldType.NUMBER.name, "IU/L | 5 – 35 IU/L", "", "LFT / RFT", ""),
            TemplateField("total_bilirubin", "Total Bilirubin", FieldType.NUMBER.name, "mg/dL | 0.2 – 0.8 mg/dL", "", "LFT / RFT", ""),
            TemplateField("total_protein", "Total Protein", FieldType.NUMBER.name, "g/dL | 6.0 – 8.0 g/dL", "", "LFT / RFT", ""),
            TemplateField("albumin", "Albumin", FieldType.NUMBER.name, "g/dL | 3.5 – 5.0 g/dL", "", "LFT / RFT", ""),
            TemplateField("globulin", "Globulin", FieldType.NUMBER.name, "g/dL | 2.5 – 3.0 g/dL", "", "LFT / RFT", ""),
            TemplateField("alkaline_phosphatase", "Alkaline Phosphatase", FieldType.NUMBER.name, "U/L | 80 – 306 U/L", "", "LFT / RFT", ""),
            TemplateField("calcium", "Calcium", FieldType.NUMBER.name, "mg/dL | 9.0 – 11.0 mg/dL", "", "LFT / RFT", ""),
            TemplateField("phosphorus", "Phosphorus (Inorganic)", FieldType.NUMBER.name, "mg/dL | 2.0 – 4.5 mg/dL", "", "LFT / RFT", ""),
            TemplateField("uric_acid", "Uric Acid", FieldType.NUMBER.name, "mg/dL | 2.0 – 6.5 mg/dL", "", "LFT / RFT", ""),
            TemplateField("amylase", "Amylase", FieldType.NUMBER.name, "U/L | Up to 90 U/L", "", "LFT / RFT", ""),
            TemplateField("ldh", "LDH", FieldType.NUMBER.name, "IU/L | 235 – 470 IU/L", "", "LFT / RFT", ""),
            TemplateField("sodium", "Sodium", FieldType.NUMBER.name, "mEq/L | 135 – 145 mEq/L", "", "LFT / RFT", ""),
            TemplateField("potassium", "Potassium", FieldType.NUMBER.name, "mEq/L | 3.5 – 5.0 mEq/L", "", "LFT / RFT", ""),
            TemplateField("chloride", "Chloride", FieldType.NUMBER.name, "mEq/L | 95 – 105 mEq/L", "", "LFT / RFT", ""),

            // LIPID PROFILE
            TemplateField("triglycerides", "Triglycerides", FieldType.NUMBER.name, "mg/dL | 3 – 150 mg/dL", "", "LIPID PROFILE", ""),
            TemplateField("total_cholesterol", "Total Cholesterol", FieldType.NUMBER.name, "mg/dL | 130 – 230 mg/dL", "", "LIPID PROFILE", ""),
            TemplateField("hdl_cholesterol", "HDL Cholesterol", FieldType.NUMBER.name, "mg/dL | 40 – 60 mg/dL", "", "LIPID PROFILE", ""),
            TemplateField("vldl_cholesterol", "VLDL Cholesterol", FieldType.NUMBER.name, "mg/dL | 12 – 35 mg/dL", "", "LIPID PROFILE", ""),
            TemplateField("ldl_cholesterol", "LDL Cholesterol", FieldType.NUMBER.name, "mg/dL | Up to 150 mg/dL", "", "LIPID PROFILE", ""),

            // DIABETIC PROFILE
            TemplateField("fasting_blood_sugar", "Fasting Blood Sugar", FieldType.NUMBER.name, "mg/dL | 70.0 – 110.0 mg/dL", "", "DIABETIC PROFILE", ""),
            TemplateField("postprandial_blood_sugar", "Postprandial Blood Sugar", FieldType.NUMBER.name, "mg/dL | Up to 140 mg/dL", "", "DIABETIC PROFILE", ""),
            TemplateField("random_blood_sugar", "Random Blood Sugar", FieldType.NUMBER.name, "mg/dL | Up to 160 mg/dL", "", "DIABETIC PROFILE", ""),
            TemplateField("hba1c", "HbA1c (Glycosylated Hb)", FieldType.NUMBER.name, "% | 4 – 6 %", "", "DIABETIC PROFILE", "")
        )
    )

    private fun bacteriologyRequestTemplate() = FormTemplate(
        templateKey = KEY_BACTERIOLOGY_REQUEST,
        name = "Bacteriology Laboratory (Test Request Form)",
        category = FormCategory.LAB.name,
        hospitalHeader = "GRANT GOVERNMENT MEDICAL COLLEGE AND SIR J. J. GROUP OF HOSPITALS, BYCULLA, MUMBAI - 08",
        departmentSubtitle = "DEPARTMENT OF MICROBIOLOGY",
        description = "Exact GGMC Microbiology Bacteriology Laboratory Test Request Form including specimen site, antibiotics, SSI status, and investigation required.",
        footerText = "(G.C.P.) O 1180 ( 1,00,000–12-2024)",
        builtIn = true,
        fields = listOf(
            TemplateField("date_of_admission", "Date of Admission", FieldType.DATE.name, "", "", "Admission & Sample Collection", "admissionDate"),
            TemplateField("sample_collection_date", "Date of sample collection", FieldType.DATE.name, "", "", "Admission & Sample Collection", ""),
            TemplateField("sample_collection_time", "Time of Collection", FieldType.TEXT.name, "", "", "Admission & Sample Collection", ""),
            TemplateField("clinical_diagnosis", "Clinical diagnosis", FieldType.TEXT.name, "", "", "Clinical & Specimen Details", "diagnosis"),
            TemplateField("antibiotics_started", "Antibiotics started if any", FieldType.TEXT.name, "", "", "Clinical & Specimen Details", ""),
            TemplateField("specimen_type", "Specimen type", FieldType.TEXT.name, "e.g., Pus / Blood / Urine / Sputum / Swab", "", "Clinical & Specimen Details", ""),
            TemplateField("specimen_site", "Site of specimen collection (specify please)", FieldType.TEXT.name, "", "", "Clinical & Specimen Details", ""),
            TemplateField("case_of_ssi", "Case of SSI (Yes/No)", FieldType.TEXT.name, "Yes / No", "", "Clinical & Specimen Details", ""),
            TemplateField("date_of_surgery", "Date of Surgery", FieldType.DATE.name, "", "", "Clinical & Specimen Details", ""),
            TemplateField("investigation_required", "Investigation Required", FieldType.MULTILINE.name, "e.g., Gram Stain, Aerobic Culture & Sensitivity", "", "Clinical & Specimen Details", ""),
            TemplateField("request_date", "Date", FieldType.DATE.name, "", "", "Sign-Off", ""),
            TemplateField("doctor_name", "Doctor's Name (optional)", FieldType.TEXT.name, "", "", "Sign-Off", "admittingDoctor")
        )
    )

    private fun serologyRequestTemplate() = FormTemplate(
        templateKey = KEY_SEROLOGY_REQUEST,
        name = "Serology Laboratory (Test Request Form)",
        category = FormCategory.LAB.name,
        hospitalHeader = "GRANT GOVT. MEDICAL COLLEGE & SIR J. J. GROUP OF HOSPITALS, BYCULLA, MUMBAI 8",
        departmentSubtitle = "DEPARTMENT OF MICROBIOLOGY",
        description = "Exact GGMC Microbiology Serology Laboratory Test Request Form with tickable Latex/Tube Agglutination, Rapid Immunochromatography, and ELISA matrix.",
        footerText = "Date & Sign",
        builtIn = true,
        fields = listOf(
            TemplateField("mlc_prisoner_bloodbank", "MLC No./ Prisoner/Blood bank", FieldType.TEXT.name, "", "", "Request Details", "mlcNumber"),
            TemplateField("date_of_admission", "Date of Admission", FieldType.DATE.name, "", "", "Request Details", "admissionDate"),
            TemplateField("sample_collection_date", "Date of sample collection", FieldType.DATE.name, "", "", "Request Details", ""),
            TemplateField("sample_collection_time", "Time of Collection", FieldType.TEXT.name, "", "", "Request Details", ""),
            TemplateField("clinical_diagnosis", "Clinical diagnosis", FieldType.TEXT.name, "", "", "Request Details", "diagnosis"),
            TemplateField("clinical_history", "Clinical History", FieldType.MULTILINE.name, "", "", "Request Details", "extraNotes"),
            TemplateField("specimen_type", "Specimen type", FieldType.TEXT.name, "", "Serum / Blood", "Request Details", ""),

            // Column 1: Latex agglutination / Floculation / Tube agglutination
            TemplateField("latex_ra", "RA", FieldType.CHECKBOX.name, "", "No", "Latex agglutination / Floculation / Tube agglutination", ""),
            TemplateField("latex_aso", "ASO", FieldType.CHECKBOX.name, "", "No", "Latex agglutination / Floculation / Tube agglutination", ""),
            TemplateField("latex_crp", "CRP", FieldType.CHECKBOX.name, "", "No", "Latex agglutination / Floculation / Tube agglutination", ""),
            TemplateField("latex_rpr", "RPR", FieldType.CHECKBOX.name, "", "No", "Latex agglutination / Floculation / Tube agglutination", ""),
            TemplateField("latex_tube_widal", "Tube WIDAL", FieldType.CHECKBOX.name, "", "No", "Latex agglutination / Floculation / Tube agglutination", ""),

            // Column 2: Rapid Immunochromatography
            TemplateField("rapid_hbsag", "HBsAg (Rapid)", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_hcv_igm", "HCV IgM (Rapid)", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_hav_igm", "HAV IgM (Rapid)", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_hev_igm", "HEV IgM (Rapid)", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_dengue_ns1_igm_igg", "Dengue NS1Ag IgM/IgG", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_leptospira_igm", "Leptospira IgM (Rapid)", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_chikungunya_igm", "Chikungunya IgM", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),
            TemplateField("rapid_malaria_pf_pv", "Malaria Pf/Pv Antigen", FieldType.CHECKBOX.name, "", "No", "Rapid Immunochromatography", ""),

            // Column 3: ELISA
            TemplateField("elisa_hbsag", "HBsAg (ELISA)", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_hcv_igm", "HCV IgM (ELISA)", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_hav_igm", "HAV IgM (ELISA)", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_hev_igm", "HEV IgM (ELISA)", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_dengue_ns1_igm", "Dengue NS1Ag IgM", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_dengue_igm", "Dengue IgM", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),
            TemplateField("elisa_leptospira_igm", "Leptospira IgM (ELISA)", FieldType.CHECKBOX.name, "", "No", "ELISA", ""),

            // Footer
            TemplateField("request_date", "Date", FieldType.DATE.name, "", "", "Sign-Off", "")
        )
    )

    private fun hivConsentTemplate() = FormTemplate(
        templateKey = KEY_HIV_CONSENT,
        name = "Consent Form for HIV Testing",
        category = FormCategory.LAB.name,
        hospitalHeader = "Grant Medical college & Sir J.J. Group of Hospitals, Mumbai 400 008",
        departmentSubtitle = "Government of Maharashtra — Department of Microbiology",
        description = "Exact Government of Maharashtra / GGMC Consent Form for HIV Testing with verbatim counselling declaration, statutory notes, and signature blocks.",
        footerText = "Signature/Thumb impression & Doctors Signature",
        builtIn = true,
        fields = listOf(
            TemplateField("doctor_ward", "Doctor / Ward (optional override)", FieldType.TEXT.name, "Auto-fills Doctor & Ward if blank", "", "Consent Details", ""),
            TemplateField("patient_address", "Address", FieldType.MULTILINE.name, "", "", "Consent Details", "address"),
            TemplateField("patient_phone", "Phone No.", FieldType.TEXT.name, "", "", "Consent Details", "mobile"),
            TemplateField("consent_date", "Date", FieldType.DATE.name, "", "", "Consent Timing", ""),
            TemplateField("consent_time", "Time", FieldType.TEXT.name, "", "", "Consent Timing", "")
        )
    )

    private fun surgerySpecimenTemplate() = FormTemplate(
        templateKey = KEY_SURGERY_SPECIMEN,
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
        templateKey = KEY_HMIS_DISCHARGE,
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
