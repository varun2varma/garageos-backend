window.EstimateStep = {

   render() {

            return `

    <div class="card shadow-sm">

        <div class="card-header">

            <h4 class="mb-0">

                Prepare Customer Estimate

            </h4>

            <small class="text-muted">

                Review inspection findings and decide what should be included in the customer estimate.

            </small>

        </div>

        <div class="card-body">

            ${this.renderInspectionCards()}

            <hr class="my-4">

            <div class="mb-4">

                <label class="form-label">

                    Estimate Remarks

                </label>

                <textarea
                    id="estimateRemarks"
                    class="form-control"
                    rows="4"
                    placeholder="Internal remarks for estimate..."></textarea>

            </div>

            <div class="d-flex justify-content-between">

                <button
                    id="previousBtn"
                    class="btn btn-outline-secondary">

                    ← Previous

                </button>

                <button
                    id="createEstimateBtn"
                    class="btn btn-success">

                    Create Estimate

                </button>

            </div>

        </div>

    </div>

    `;

        },

    renderInspectionCards() {

        const inspections =
            WorkflowHelper.state.inspections || [];

        if (inspections.length === 0) {

            return `

    <div class="alert alert-warning">

        No inspections available.

    </div>

    `;

        }

        return inspections
            .map((inspection,index)=>
                this.inspectionCard(
                    inspection,
                    index
                )
            )
            .join("");

    },

    inspectionCard(
        inspection,
        index
    ){

        return `

    <div class="card border mb-4">

        <div class="card-header bg-light d-flex justify-content-between">

            <div>

                <strong>

                    Complaint #${index+1}

                </strong>

            </div>

            <span class="badge bg-primary">

                READY

            </span>

        </div>

        <div class="card-body">

            <div class="mb-3">

                <label class="fw-bold">

                    Customer Complaint

                </label>

                <div>

                    ${inspection.complaint}

                </div>

            </div>

            <div class="mb-3">

                <label class="fw-bold">

                    Inspection Findings

                </label>

                <div class="text-muted">

                    ${inspection.inspectionNotes}

                </div>

            </div>

            <div class="mb-3">

                <label class="fw-bold">

                    Recommended Work

                </label>

                <div class="text-success">

                    ${inspection.recommendedWork}

                </div>

            </div>

            <div class="form-check form-switch">

                <input

                    class="form-check-input include-estimate"

                    type="checkbox"

                    checked

                    data-id="${inspection.id}"

                >

                <label class="form-check-label">

                    Include this work in Estimate

                </label>

            </div>

        </div>

    </div>

    `;

    },

    bindEvents() {

        document
            .getElementById("createEstimateBtn")
            ?.addEventListener(
                "click",
                async () => {

                    const success =
                        await this.save();

                    if (!success) {

                        return;

                    }

                    alert(
                        "Estimate created successfully."
                    );

                    Workflow.nextStep();

                }
            );

    },

    validate() {

        if (!WorkflowHelper.state.jobCardId) {

            alert("Job Card not found.");

            return false;

        }

        return true;

    },

    collectData() {

        return {

            jobCardId:
                WorkflowHelper.state.jobCardId,

            remarks:
                document
                    .getElementById("estimateRemarks")
                    .value
                    .trim()

        };

    },

    async save() {

        if (!this.validate()) {

            return false;

        }

        const response =
            await WorkflowService.createEstimate(

                this.collectData()

            );

        console.log(
            "Estimate Created",
            response
        );

        return true;

    }

};