window.JobStep = {

    render() {

        const job = WorkflowHelper.state.job || {};

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4 class="mb-0">

            Create Job Card

        </h4>

    </div>

    <div class="card-body">

        <div id="complaintsContainer">

            ${this.renderComplaints()}

        </div>

        <button
            id="addComplaintBtn"
            class="btn btn-outline-primary mb-4">

            + Add Complaint

        </button>

        <div class="row">

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Odometer Reading

                </label>

                <input
                    id="odometerReading"
                    type="number"
                    class="form-control"
                    value="${job.odometerReading || ""}">

            </div>

            <div class="col-md-6 mb-3">

                <label class="form-label">

                    Estimated Delivery Date

                </label>

                <input
                    id="estimatedDeliveryDate"
                    type="date"
                    class="form-control"
                    value="${job.estimatedDeliveryDate || ""}">

            </div>

        </div>

        <div class="mb-3">

            <label class="form-label">

                Remarks

            </label>

            <textarea
                id="remarks"
                rows="3"
                class="form-control">${job.remarks || ""}</textarea>

        </div>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                ← Previous

            </button>

            <button
                id="createJobBtn"
                class="btn btn-success">

                ${WorkflowHelper.state.jobCardId ? "Update Job Card" : "Create Job Card"}

            </button>

        </div>

    </div>

</div>

`;

    },

    renderComplaints() {

        const job = WorkflowHelper.state.job;

        if (
            job &&
            job.complaints &&
            job.complaints.length > 0
        ) {

            return job.complaints
                .map(c => this.complaintRow(c.complaint))
                .join("");

        }

        return this.complaintRow();

    },

    complaintRow(value = "") {

        return `

<div class="row complaint-row mb-3">

    <div class="col-md-10">

        <input
            class="form-control complaint-input"
            placeholder="Enter customer complaint"
            value="${value}">

    </div>

    <div class="col-md-2 d-grid">

        <button
            type="button"
            class="btn btn-outline-danger removeComplaint">

            Remove

        </button>

    </div>

</div>

`;

    },

    bindEvents() {

        document
            .getElementById("previousBtn")
            ?.addEventListener("click", () => {

                Workflow.previousStep();

            });

        document
            .getElementById("addComplaintBtn")
            ?.addEventListener("click", () => {

                document
                    .getElementById("complaintsContainer")
                    .insertAdjacentHTML(
                        "beforeend",
                        this.complaintRow()
                    );

                this.bindRemoveButtons();

            });

        this.bindRemoveButtons();

        document
            .getElementById("createJobBtn")
            ?.addEventListener("click", async () => {

                const success =
                    await this.save();

                if (success) {

                    Workflow.nextStep();

                }

            });

    },

    bindRemoveButtons() {

        document
            .querySelectorAll(".removeComplaint")
            .forEach(button => {

                button.onclick = () => {

                    const rows =
                        document.querySelectorAll(".complaint-row");

                    if (rows.length === 1) {

                        return;

                    }

                    button
                        .closest(".complaint-row")
                        .remove();

                };

            });

    },
        collectData() {

            const complaints = [];

            document
                .querySelectorAll(".complaint-input")
                .forEach(input => {

                    const value = input.value.trim();

                    if (value.length > 0) {

                        complaints.push({

                            complaint: value,

                            status: "OPEN"

                        });

                    }

                });

            return {

                vehicleId:
                    WorkflowHelper.state.vehicleId,

                odometerReading:
                    Number(
                        document.getElementById("odometerReading").value
                    ),

                complaints,

                estimatedDeliveryDate:
                    document.getElementById("estimatedDeliveryDate").value,

                remarks:
                    document
                        .getElementById("remarks")
                        .value
                        .trim()

            };

        },

        validate(request) {

            if (!request.vehicleId) {

                alert("Vehicle information is missing.");

                return false;

            }

            if (request.complaints.length === 0) {

                alert("Please enter at least one complaint.");

                return false;

            }

            if (!request.odometerReading || request.odometerReading <= 0) {

                alert("Please enter a valid odometer reading.");

                return false;

            }

            if (!request.estimatedDeliveryDate) {

                alert("Please select estimated delivery date.");

                return false;

            }

            return true;

        },

        async save() {

            const request = this.collectData();

            if (!this.validate(request)) {

                return false;

            }

            try {

                let job;

                if (WorkflowHelper.state.jobCardId) {

                    const response =
                        await JobCardService.updateJob(

                            WorkflowHelper.state.jobCardId,

                            request

                        );

                    job = response.data;

                }

                else {

                    job =
                        await WorkflowService.createJob(request);

                }

                WorkflowHelper.state.job = job;
                WorkflowHelper.state.jobCardId = job.id;
                WorkflowHelper.state.jobCardNumber = job.jobCardNumber;

                console.log("Job Card Saved", job);

                return true;

            }

            catch (e) {

                console.error(e);

                alert(e.message || "Unable to create Job Card.");

                return false;

            }

        }

    };