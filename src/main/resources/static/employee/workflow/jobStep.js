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

        <div id="recommendedInspectionContainer" class="mb-3"></div>

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

        <!-- MEDIA -->

        <div class="card border-0 bg-light mb-4">

            <div class="card-body">

                <h6 class="fw-bold mb-3">

                    <i class="bi bi-camera"></i>

                    Vehicle Media

                </h6>

                <div class="row">

                    <div class="col-md-4 mb-3">

                        <label class="form-label">

                            Media Stage

                        </label>

                        <select
                            id="mediaStage"
                            class="form-select">

                            <option value="BEFORE_SERVICE">

                                Before Service

                            </option>

                            <option value="DURING_REPAIR">

                                During Repair

                            </option>

                            <option value="AFTER_REPAIR">

                                After Repair

                            </option>

                        </select>

                    </div>

                    <div class="col-md-8 mb-3">

                        <label class="form-label">

                            Photo / Video

                        </label>

                        <input
                            id="mediaFile"
                            type="file"
                            class="form-control"
                            accept="image/*,video/*">

                    </div>

                </div>

                <div
                    id="mediaUploadMessage"
                    class="small text-muted">

                    Media will be uploaded after the Job Card is created.

                </div>

            </div>

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

                ${WorkflowHelper.state.jobCardId
                    ? "Update Job Card"
                    : "Create Job Card"}

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

    renderRecommendations() {

        const container =
            document.getElementById(
                "recommendedInspectionContainer"
            );

        if (!container) return;

        const items =
            WorkflowHelper.state
                .recommendedInspectionItems || [];

        container.innerHTML = "";

        if (items.length === 0) return;

        container.innerHTML =
            `<label class="form-label">
                Recommended Inspection Items
            </label>`;

        items.forEach(item => {

            container.innerHTML += `
                <div class="d-flex justify-content-between align-items-center border rounded p-2 mb-2">

                    <span>${item.checkItem}</span>

                    <button
                        class="btn btn-sm btn-outline-primary"
                        onclick="JobStep.addRecommendation('${item.checkItem}')">

                        +

                    </button>

                </div>
            `;

        });

    },

    addRecommendation(name) {

        const exists =
            [...document.querySelectorAll(".complaint-input")]
                .some(
                    input =>
                        input.value.trim() === name
                );

        if (exists) {
            return;
        }

        document
            .getElementById("complaintsContainer")
            .insertAdjacentHTML(
                "beforeend",
                this.complaintRow(name)
            );

        this.bindRemoveButtons();

    },

    bindEvents() {

        this.renderRecommendations();

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

                const button =
                    document.getElementById(
                        "createJobBtn"
                    );

                button.disabled = true;

                const originalText =
                    button.innerHTML;

                button.innerHTML =
                    `<i class="bi bi-hourglass-split"></i>
                     Saving...`;

                const success =
                    await this.save();

                if (success) {

                    Workflow.nextStep();

                } else {

                    button.disabled = false;

                    button.innerHTML =
                        originalText;

                }

            });

    },

    bindRemoveButtons() {

        document
            .querySelectorAll(".removeComplaint")
            .forEach(button => {

                button.onclick = () => {

                    const rows =
                        document.querySelectorAll(
                            ".complaint-row"
                        );

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

                const value =
                    input.value.trim();

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
                    document
                        .getElementById(
                            "odometerReading"
                        )
                        .value
                ),

            complaints,

            estimatedDeliveryDate:
                document
                    .getElementById(
                        "estimatedDeliveryDate"
                    )
                    .value,

            remarks:
                document
                    .getElementById("remarks")
                    .value
                    .trim()

        };

    },

    validate(request) {

        if (!request.vehicleId) {

            alert(
                "Vehicle information is missing."
            );

            return false;

        }

        if (request.complaints.length === 0) {

            alert(
                "Please enter at least one complaint."
            );

            return false;

        }

        if (
            !request.odometerReading ||
            request.odometerReading <= 0
        ) {

            alert(
                "Please enter a valid odometer reading."
            );

            return false;

        }

        if (!request.estimatedDeliveryDate) {

            alert(
                "Please select estimated delivery date."
            );

            return false;

        }

        return true;

    },

    async save() {

        const request =
            this.collectData();

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

                job = response;

            }

            else {

                job =
                    await WorkflowService.createJob(
                        request
                    );

            }

            WorkflowHelper.state.job =
                job;

            WorkflowHelper.state.jobCardId =
                job.id;

            WorkflowHelper.state.jobCardNumber =
                job.jobCardNumber;

            console.log(
                "Job Card Saved",
                job
            );

            /*
             * -----------------------------------------------------
             * MEDIA UPLOAD
             * -----------------------------------------------------
             */

            const mediaFile =
                document.getElementById(
                    "mediaFile"
                )?.files?.[0];

            const mediaStage =
                document.getElementById(
                    "mediaStage"
                )?.value;

            if (mediaFile) {

                const message =
                    document.getElementById(
                        "mediaUploadMessage"
                    );

                if (message) {

                    message.innerHTML =
                        `<span class="text-muted">

                            <i class="bi bi-cloud-upload"></i>

                            Uploading media...

                        </span>`;

                }

                const formData =
                    new FormData();

                formData.append(
                    "stage",
                    mediaStage
                );

                formData.append(
                    "file",
                    mediaFile
                );

                /*
                 * Use the SAME JWT authentication mechanism
                 * used by window.Api.
                 *
                 * IMPORTANT:
                 * Do not set Content-Type manually.
                 * Browser will set multipart/form-data boundary.
                 */

                const token =
                    window.Auth
                        ? Auth.getAccessToken()
                        : null;

                const headers = {};

                if (token) {

                    headers.Authorization =
                        `Bearer ${token}`;

                }

                const response =
                    await fetch(
                        `/api/v1/job-cards/${job.id}/media`,
                        {
                            method: "POST",

                            headers,

                            body: formData
                        }
                    );

                if (!response.ok) {

                    let message =
                        `Media upload failed (${response.status})`;

                    try {

                        const error =
                            await response.json();

                        message =
                            error.message ||
                            error.error ||
                            message;

                    }

                    catch (e) {

                        const text =
                            await response.text();

                        if (text) {

                            message = text;

                        }

                    }

                    throw new Error(message);

                }

                const media =
                    await response.json();

                console.log(
                    "Media Uploaded",
                    media
                );

                if (message) {

                    message.innerHTML =
                        `<span class="text-success">

                            <i class="bi bi-check-circle-fill"></i>

                            Media uploaded successfully.

                        </span>`;

                }

            }

            return true;

        }

        catch (e) {

            console.error(e);

            alert(
                e.message ||
                "Unable to create Job Card."
            );

            return false;

        }

    }

};