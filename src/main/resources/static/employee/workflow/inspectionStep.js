window.InspectionStep = {

    render() {

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Vehicle Inspection

        </h4>

    </div>

    <div class="card-body">

        ${this.renderComplaintCards()}

        <div class="d-flex justify-content-between mt-4">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                ← Previous

            </button>

            <button
                id="saveInspectionBtn"
                class="btn btn-primary">

                Save All Inspections

            </button>

        </div>

    </div>

</div>

`;

    },

    renderComplaintCards() {

        const complaints =
            WorkflowHelper.state.job?.complaints || [];

        if (complaints.length === 0) {

            return `

<div class="alert alert-warning">

    No complaints available.

</div>

`;

        }

        return complaints
            .map((complaint, index) =>
                this.inspectionCard(
                    complaint,
                    index
                ))
            .join("");

    },

    inspectionCard(
        complaint,
        index
    ) {

        return `

<div class="card border mb-4">

    <div class="card-header bg-light">

        <div class="d-flex justify-content-between">

            <div>

                <strong>

                    Complaint #${index + 1}

                </strong>

            </div>

            <span class="badge bg-primary">

                ${complaint.status}

            </span>

        </div>

        <div class="mt-2">

            ${complaint.complaint}

        </div>

    </div>

    <div class="card-body">

        <div class="mb-3">

            <label class="form-label">

                Inspection Notes

            </label>

            <textarea

                class="form-control inspection-notes"

                rows="4"

                data-id="${complaint.id}"

                placeholder="Inspection findings">

            </textarea>

        </div>

        <div>

            <label class="form-label">

                Recommended Work

            </label>

            <textarea

                class="form-control recommended-work"

                rows="4"

                data-id="${complaint.id}"

                placeholder="Recommended repairs">

            </textarea>

        </div>

    </div>

</div>

`;

    },
        bindEvents() {

            document
                .getElementById("saveInspectionBtn")
                ?.addEventListener(
                    "click",
                    async () => {

                        const success =
                            await this.save();

                        if (!success) {

                            return;

                        }

                        alert(
                            "Inspection saved successfully."
                        );

                        Workflow.nextStep();

                    }
                );

        },

        validate() {

            const inspections =
                this.collectData();

            for (let i = 0; i < inspections.length; i++) {

                const inspection =
                    inspections[i];

                if (
                    !inspection.inspectionNotes ||
                    inspection.inspectionNotes.trim().length === 0
                ) {

                    alert(
                        `Inspection Notes are mandatory for Complaint #${i + 1}`
                    );

                    return false;

                }

                if (
                    !inspection.recommendedWork ||
                    inspection.recommendedWork.trim().length === 0
                ) {

                    alert(
                        `Recommended Work is mandatory for Complaint #${i + 1}`
                    );

                    return false;

                }

            }

            return true;

        },

        collectData() {

            const inspections = [];

            const complaints =
                WorkflowHelper.state.job?.complaints || [];

            complaints.forEach(complaint => {

                const notesElement =
                    document.querySelector(
                        `.inspection-notes[data-id="${complaint.id}"]`
                    );

                const workElement =
                    document.querySelector(
                        `.recommended-work[data-id="${complaint.id}"]`
                    );

                inspections.push({

                    complaintId: complaint.id,

                    inspectionNotes:
                        notesElement
                            ? notesElement.value.trim()
                            : "",

                    recommendedWork:
                        workElement
                            ? workElement.value.trim()
                            : ""

                });

            });

            return inspections;

        },
            async save() {

                if (!this.validate()) {

                    return false;

                }

                try {

                    const inspections =
                        this.collectData();

                    const savedInspections = [];

                    for (const inspection of inspections) {

                        const request = {

                            inspectionNotes:
                                inspection.inspectionNotes,

                            recommendedWork:
                                inspection.recommendedWork

                        };

                        const response =
                            await WorkflowService.createInspection(

                                inspection.complaintId,

                                request

                            );

                        savedInspections.push(response);

                    }

                    WorkflowHelper.state.inspections =
                        savedInspections;

                    console.log(
                        "All inspections saved.",
                        savedInspections
                    );

                    return true;

                }

                catch (e) {

                    console.error(e);

                    alert(
                        e.message ||
                        "Unable to save inspections."
                    );

                    return false;

                }

            }

        };