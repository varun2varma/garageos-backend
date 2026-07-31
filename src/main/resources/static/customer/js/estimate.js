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

            this.render();

        } catch (error) {

            console.error(error);

            this.renderEmpty();

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render() {

        const container =
            document.getElementById(
                "estimateContainer"
            );

        if (!container) {

            return;

        }

        if (!this.estimates.length) {

            this.renderEmpty();

            return;

        }

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

        container.innerHTML =

            this.estimates

                .map(

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

                        <div class="col-md-6">

                            <div class="estimate-info">

                                <small class="text-muted">

                                    Estimate Number

                                </small>

                                <div class="fw-semibold">

                                    ${estimate.estimateNumber}

                                </div>

                            </div>

                        </div>

                        <div class="col-md-6">

                            <div class="estimate-info">

                                <small class="text-muted">

                                    Job Card Number

                                </small>

                                <div class="fw-semibold">

                                    ${estimate.jobCardNumber}

                                </div>

                            </div>

                        </div>

                    </div>

                    <div class="mt-4">

                        <div class="estimate-total-card">

                            <small class="text-muted">

                                Grand Total

                            </small>

                            <h2 class="text-success mt-2 mb-0">

                                ${this.formatCurrency(
                                    estimate.grandTotal
                                )}

                            </h2>

                        </div>

                    </div>

                    <div class="mt-4">

                        ${
                            estimate.status === "PENDING"

                            ?

                            `

                            <div class="row g-2">

                                <div class="col-6">

                                    <button

                                        class="btn btn-success w-100"

                                        onclick="CustomerEstimate.approve(${estimate.id})">

                                        <i class="bi bi-check-circle me-2"></i>

                                        Approve

                                    </button>

                                </div>

                                <div class="col-6">

                                    <button

                                        class="btn btn-danger w-100"

                                        onclick="CustomerEstimate.reject(${estimate.id})">

                                        <i class="bi bi-x-circle me-2"></i>

                                        Reject

                                    </button>

                                </div>

                            </div>

                            `

                            :

                            `

                            <div class="alert alert-light text-center mb-0">

                                Estimate already

                                <strong>

                                    ${this.formatStatus(
                                        estimate.status
                                    )}

                                </strong>

                            </div>

                            `

                        }

                    </div>

                </div>

            </div>

            `

                            )

                            .join("");

                },

                renderEmpty() {

                    const container =
                        document.getElementById(
                            "estimateContainer"
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

                                    No estimates are currently awaiting your approval.

                                </p>

                            </div>

                        </div>

                    `;

                },

                async approve(id) {

                    try {

                        CustomerApp.showLoading();

                        await EstimateService
                            .approveEstimate(id);

                        await this.load();

                    } catch (error) {

                        console.error(error);

                        alert(
                            "Unable to approve estimate."
                        );

                    } finally {

                        CustomerApp.hideLoading();

                    }

                },

                async reject(id) {

                    try {

                        CustomerApp.showLoading();

                        await EstimateService
                            .rejectEstimate(id);

                        await this.load();

                    } catch (error) {

                        console.error(error);

                        alert(
                            "Unable to reject estimate."
                        );

                    } finally {

                        CustomerApp.hideLoading();

                    }

                },

                statusBadge(status) {

                    switch (status) {

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

                        case "PENDING":

                            return `
                                <span class="badge bg-warning text-dark">
                                    Pending
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

                formatStatus(status) {

                    if (!status) {

                        return "-";

                    }

                    return status
                        .replaceAll("_", " ")
                        .toLowerCase()
                        .replace(/\b\w/g, c => c.toUpperCase());

                },

                formatCurrency(amount) {

                    return new Intl.NumberFormat(

                        "en-IN",

                        {

                            style: "currency",

                            currency: "INR",

                            minimumFractionDigits: 2,

                            maximumFractionDigits: 2

                        }

                    ).format(amount ?? 0);

                }

            };

