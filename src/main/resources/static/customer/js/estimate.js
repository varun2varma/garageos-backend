window.CustomerEstimate = {

    estimates: [],

    async init() {

        document
            .getElementById(
                "refreshEstimateButton"
            )
            ?.addEventListener(
                "click",
                () => this.load()
            );

        await this.load();

    },

    async load() {

        try {

            CustomerApp.showLoading();

            this.estimates =

                await CustomerPortalService
                    .getEstimates();

            this.loadStatistics();

            this.renderEstimateList(
                this.estimates
            );

            const selectedJobCard =

                sessionStorage.getItem(
                    "selectedJobCard"
                );

            if (selectedJobCard) {

                sessionStorage.removeItem(
                    "selectedJobCard"
                );

                const estimate =

                    this.estimates.find(

                        estimate =>

                            estimate.jobCardNumber ===
                            selectedJobCard

                    );

                if (estimate) {

                    await this.viewEstimate(
                        estimate.id
                    );

                }

            }

        }

        catch (error) {

            console.error(error);

            this.renderEmpty();

        }

        finally {

            CustomerApp.hideLoading();

        }

    },

    loadStatistics() {

        document.getElementById(
            "estimateTotal"
        ).textContent =

            this.estimates.length;

        document.getElementById(
            "estimatePending"
        ).textContent =

            this.estimates.filter(

                estimate =>

                    estimate.status ===
                    "PENDING"

            ).length;

        document.getElementById(
            "estimateApproved"
        ).textContent =

            this.estimates.filter(

                estimate =>

                    estimate.status ===
                    "APPROVED"

            ).length;

        document.getElementById(
            "estimateRejected"
        ).textContent =

            this.estimates.filter(

                estimate =>

                    estimate.status ===
                    "REJECTED"

            ).length;

    },

    renderEmpty() {

        const container =

            document.getElementById(
                "estimateListContainer"
            );

        if (!container) {

            return;

        }

        container.innerHTML = `

            <div class="col-12">

                <div class="customer-card text-center py-5">

                    <i class="bi bi-patch-check-fill display-3 text-success"></i>

                    <h3 class="mt-4">

                        You're All Caught Up!

                    </h3>

                    <p class="text-muted">

                        No estimates are currently available.

                    </p>

                </div>

            </div>

        `;

    },

    renderEstimateList(estimates) {

        const container =

            document.getElementById(
                "estimateListContainer"
            );

        if (!container) {

            return;

        }

        if (!estimates.length) {

            this.renderEmpty();

            return;

        }

        container.innerHTML =

            estimates.map(

                estimate => `

                <div class="col-xl-6 col-lg-6">

                    <div class="customer-card estimate-card h-100">

                        <div class="estimate-header d-flex justify-content-between align-items-center">

                            <div class="d-flex align-items-center">

                                <div class="estimate-icon me-3">

                                    <i class="bi bi-receipt-cutoff fs-2 text-primary"></i>

                                </div>

                                <div>

                                    <h5 class="mb-1">

                                        ${estimate.estimateNumber}

                                    </h5>

                                    <small class="text-muted">

                                        Job Card

                                        ${estimate.jobCardNumber}

                                    </small>

                                </div>

                            </div>

                            <div>

                                ${this.statusBadge(
                                    estimate.status
                                )}

                            </div>

                        </div>

                        <hr>

                        <div class="row">

                            <div class="col-6">

                                <small class="text-muted">

                                    Estimate Number

                                </small>

                                <div class="fw-semibold">

                                    ${estimate.estimateNumber}

                                </div>

                            </div>

                            <div class="col-6">

                                <small class="text-muted">

                                    Grand Total

                                </small>

                                <div class="fw-bold text-success">

                                    ${this.formatCurrency(
                                        estimate.grandTotal
                                    )}

                                </div>

                            </div>

                        </div>

                        <div class="mt-4">

                            <button

                                class="btn btn-primary w-100"

                                onclick="CustomerEstimate.viewEstimate(${estimate.id})">

                                <i class="bi bi-eye me-2"></i>

                                View Estimate

                            </button>

                        </div>

                    </div>

                </div>

                `

            ).join("");

        document
            .getElementById(
                "estimateListContainer"
            )
            .classList
            .remove("d-none");

        document
            .getElementById(
                "estimateDetailsContainer"
            )
            .classList
            .add("d-none");

    },

    showEstimateList() {

        document
            .getElementById(
                "estimateDetailsContainer"
            )
            .classList
            .add("d-none");

        document
            .getElementById(
                "estimateListContainer"
            )
            .classList
            .remove("d-none");

    },
    async viewEstimate(estimateId) {

        try {

            CustomerApp.showLoading();

            const response =
                await CustomerPortalService.getEstimate(
                    estimateId
                );

            const estimate =
                response.estimate;

            const items =
                response.items || [];

            if (!estimate) {

                return;

            }

            document
                .getElementById(
                    "estimateListContainer"
                )
                .classList
                .add("d-none");

            document
                .getElementById(
                    "estimateDetailsContainer"
                )
                .classList
                .remove("d-none");

            const container =

                document.getElementById(
                    "estimateDetailsContent"
                );

            container.innerHTML = `

                <div class="customer-card">

                    <div class="d-flex justify-content-between align-items-start mb-4">

                        <div>

                            <h3>

                                ${estimate.estimateNumber}

                            </h3>

                            <div class="text-muted">

                                ${estimate.jobCardNumber}

                            </div>

                        </div>

                        ${this.statusBadge(
                            estimate.status
                        )}

                    </div>

                    <div class="row mb-4">

                        <div class="col-md-4">

                            <strong>

                                Estimate Number

                            </strong>

                            <div>

                                ${estimate.estimateNumber}

                            </div>

                        </div>

                        <div class="col-md-4">

                            <strong>

                                Job Card

                            </strong>

                            <div>

                                ${estimate.jobCardNumber}

                            </div>

                        </div>

                        <div class="col-md-4">

                            <strong>

                                Grand Total

                            </strong>

                            <div class="fw-bold text-success">

                                ${this.formatCurrency(
                                    estimate.grandTotal
                                )}

                            </div>

                        </div>

                    </div>

                    <hr>

                    <h5 class="mb-4">

                        Estimate Items

                    </h5>

                    <table class="table table-bordered align-middle">

                        <thead class="table-light">

                            <tr>

                                <th>#</th>

                                <th>Description</th>

                                <th>Qty</th>

                                <th>Unit Price</th>

                                <th>Total</th>

                            </tr>

                        </thead>

                        <tbody>

                            ${items.map((item,index)=>`

                                <tr>

                                    <td>

                                        ${index+1}

                                    </td>

                                    <td>

                                        ${item.description}

                                    </td>

                                    <td>

                                        ${item.quantity}

                                    </td>

                                    <td>

                                        ${this.formatCurrency(item.unitPrice)}

                                    </td>

                                    <td class="fw-bold">

                                        ${this.formatCurrency(item.totalPrice)}

                                    </td>

                                </tr>

                            `).join("")}

                        </tbody>

                    </table>

                    <div class="row mt-4">

                        <div class="col-md-5 ms-auto">

                            <table class="table">

                                <tr>

                                    <th>

                                        Subtotal

                                    </th>

                                    <td class="text-end">

                                        ${this.formatCurrency(
                                            estimate.subtotal
                                        )}

                                    </td>

                                </tr>

                                <tr>

                                    <th>

                                        GST

                                    </th>

                                    <td class="text-end">

                                        ${this.formatCurrency(
                                            estimate.gst
                                        )}

                                    </td>

                                </tr>

                                <tr class="table-success">

                                    <th>

                                        Grand Total

                                    </th>

                                    <th class="text-end">

                                        ${this.formatCurrency(
                                            estimate.grandTotal
                                        )}

                                    </th>

                                </tr>

                            </table>

                        </div>

                    </div>

                    ${estimate.status === "WAITING_FOR_APPROVAL" ? `

                        <div class="mt-4 d-flex gap-3">

                            <button

                                class="btn btn-success"

                                onclick="CustomerEstimate.approveEstimate(${estimate.id})">

                                <i class="bi bi-check-circle me-2"></i>

                                Approve

                            </button>

                            <button

                                class="btn btn-danger"

                                onclick="CustomerEstimate.rejectEstimate(${estimate.id})">

                                <i class="bi bi-x-circle me-2"></i>

                                Reject

                            </button>

                        </div>

                    ` : ""}

                </div>

            `;

        }

        catch (error) {

            console.error(error);

        }

        finally {

            CustomerApp.hideLoading();

        }

    },
    async approveEstimate(estimateId) {

        try {

            await CustomerPortalService
                .approveEstimate(
                    estimateId
                );

            await this.load();

            await this.viewEstimate(
                estimateId
            );

        }

        catch (error) {

            console.error(error);

        }

    },

    async rejectEstimate(estimateId) {

        try {

            await CustomerPortalService
                .rejectEstimate(
                    estimateId
                );

            await this.load();

            await this.viewEstimate(
                estimateId
            );

        }

        catch (error) {

            console.error(error);

        }

    },

    statusBadge(status) {

        switch (status) {

            case "WAITING_FOR_APPROVAL":

                return `

                    <span class="badge bg-warning text-dark">

                        Waiting For Approval

                    </span>

                `;

            case "APPROVED":

                return `

                    <span class="badge bg-success">

                        Approved

                    </span>

                `;

            case "REJECTED":

                return `

                    <span class="badge bg-danger">

                        Rejected

                    </span>

                `;

            default:

                return `

                    <span class="badge bg-secondary">

                        ${this.formatStatus(status)}

                    </span>

                `;

        }

    },

    formatCurrency(amount) {

        return new Intl.NumberFormat(

            "en-IN",

            {

                style: "currency",

                currency: "INR"

            }

        ).format(

            amount ?? 0

        );

    },

    formatStatus(status) {

        if (!status) {

            return "-";

        }

        return status

            .replaceAll("_", " ")

            .toLowerCase()

            .replace(

                /\b\w/g,

                c => c.toUpperCase()

            );

    }

};