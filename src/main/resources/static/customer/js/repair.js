window.CustomerRepair = {

    jobCards: [],

    selectedJobCard: null,

    async init() {

        document
            .getElementById(
                "refreshRepairButton"
            )
            ?.addEventListener(
                "click",
                () => this.loadJobCards()
            );

        await this.loadJobCards();

    },

    async loadJobCards() {

        try {

            CustomerApp.showLoading();

            this.jobCards =
                await CustomerPortalService.getJobCards();

            this.loadStatistics();

            this.renderJobCards(
                this.jobCards
            );

            const selectedJobCard =
                sessionStorage.getItem(
                    "selectedJobCard"
                );

            if (selectedJobCard) {

                sessionStorage.removeItem(
                    "selectedJobCard"
                );

                await this.trackRepair(
                    selectedJobCard
                );

            }

        }
        catch (e) {

            console.error(e);

            this.renderEmpty();

        }
        finally {

            CustomerApp.hideLoading();

        }

    },

    loadStatistics() {

        document.getElementById(
            "jobCardCount"
        ).textContent =
            this.jobCards.length;

        document.getElementById(
            "repairInProgressCount"
        ).textContent =

            this.jobCards.filter(

                job =>

                    job.status !== "CLOSED"

            ).length;

        document.getElementById(
            "completedRepairCount"
        ).textContent =

            this.jobCards.filter(

                job =>

                    job.status === "CLOSED"

            ).length;

        document.getElementById(
            "pendingRepairCount"
        ).textContent =

            this.jobCards.filter(

                job =>

                    job.status === "OPEN"

            ).length;

    },

    renderEmpty() {

        const container =

            document.getElementById(
                "repairListContainer"
            );

        if (!container) {

            return;

        }

        container.innerHTML = `

            <div class="col-12">

                <div class="customer-card text-center p-5">

                    <i class="bi bi-tools display-3 text-secondary"></i>

                    <h4 class="mt-3">

                        No Repair Jobs Found

                    </h4>

                    <p class="text-muted">

                        There are no repair jobs available.

                    </p>

                </div>

            </div>

        `;

    },

        renderJobCards(jobCards) {

            const container =

                document.getElementById(
                    "repairListContainer"
                );

            if (!container) {

                return;

            }

            if (!jobCards.length) {

                this.renderEmpty();

                return;

            }

            container.innerHTML =

                jobCards
                    .map(job =>

                        this.buildRepairCard(job)

                    )
                    .join("");

            document
                .getElementById(
                    "repairListContainer"
                )
                .classList
                .remove("d-none");

            document
                .getElementById(
                    "repairDetailsContainer"
                )
                .classList
                .add("d-none");

        },

        buildRepairCard(job) {

            return `

                <div class="col-lg-6">

                    <div class="customer-card h-100">

                        <div class="d-flex justify-content-between align-items-start">

                            <div>

                                <h4 class="mb-1">

                                    ${job.registrationNumber}

                                </h4>

                                <div class="text-muted">

                                    ${job.jobCardNumber}

                                </div>

                            </div>

                            <span class="badge bg-primary">

                                ${this.formatStatus(job.status)}

                            </span>

                        </div>

                        <hr>

                        <div class="row">

                            <div class="col-6">

                                <strong>

                                    Service Date

                                </strong>

                                <div>

                                    ${job.serviceDate ?? "-"}

                                </div>

                            </div>

                            <div class="col-6">

                                <strong>

                                    Estimated Delivery

                                </strong>

                                <div>

                                    ${job.estimatedDeliveryDate ?? "-"}

                                </div>

                            </div>

                        </div>

                        <div class="mt-4">

                            <button

                                class="btn btn-primary w-100"

                                onclick="CustomerRepair.trackRepair('${job.jobCardNumber}')">

                                <i class="bi bi-search me-2"></i>

                                Track Repair

                            </button>

                        </div>

                    </div>

                </div>

            `;

        },

        showRepairList() {

            document
                .getElementById(
                    "repairDetailsContainer"
                )
                .classList
                .add("d-none");

            document
                .getElementById(
                    "repairListContainer"
                )
                .classList
                .remove("d-none");

        },

            async trackRepair(jobCardNumber) {

                try {

                    CustomerApp.showLoading();

                    const tracking =

                        await CustomerPortalService
                            .getRepairTracking(jobCardNumber);

                    const job =

                        this.jobCards.find(

                            j =>

                                j.jobCardNumber === jobCardNumber

                        );

                    if (!job) {

                        return;

                    }

                    document
                        .getElementById(
                            "repairListContainer"
                        )
                        .classList
                        .add("d-none");

                    document
                        .getElementById(
                            "repairDetailsContainer"
                        )
                        .classList
                        .remove("d-none");

                    const details =

                        document.getElementById(
                            "repairDetailsContent"
                        );

                    details.innerHTML = `

                        <div class="customer-card">

                            <div class="d-flex justify-content-between align-items-start mb-4">

                                <div>

                                    <h3>

                                        ${job.registrationNumber}

                                    </h3>

                                    <div class="text-muted">

                                        ${job.jobCardNumber}

                                    </div>

                                </div>

                                <span class="badge bg-primary">

                                    ${this.formatStatus(job.status)}

                                </span>

                            </div>

                            <div class="row mb-4">

                                <div class="col-md-4">

                                    <strong>

                                        Service Date

                                    </strong>

                                    <div>

                                        ${job.serviceDate ?? "-"}

                                    </div>

                                </div>

                                <div class="col-md-4">

                                    <strong>

                                        Estimated Delivery

                                    </strong>

                                    <div>

                                        ${job.estimatedDeliveryDate ?? "-"}

                                    </div>

                                </div>

                                <div class="col-md-4">

                                    <strong>

                                        Registration

                                    </strong>

                                    <div>

                                        ${job.registrationNumber}

                                    </div>

                                </div>

                            </div>

                            <hr>

                            <h4 class="mb-4">

                                Repair Progress

                            </h4>

                            ${this.buildStep(

                                "Inspection Completed",

                                tracking.inspectionCompleted

                            )}

                            ${this.buildStep(

                                "Estimate Prepared",

                                tracking.estimatePrepared

                            )}

                            ${this.buildStep(

                                "Estimate Approved",

                                tracking.estimateApproved

                            )}

                            ${this.buildStep(

                                "Repair Completed",

                                tracking.repairCompleted

                            )}

                            ${this.buildStep(

                                "Quality Checked",

                                tracking.qualityChecked

                            )}

                            ${this.buildStep(

                                "Invoice Generated",

                                tracking.invoiceGenerated

                            )}

                            ${this.buildStep(

                                "Payment Completed",

                                tracking.paymentCompleted

                            )}

                        </div>

                    `;

                }

                catch (e) {

                    console.error(e);

                }

                finally {

                    CustomerApp.hideLoading();

                }

            },

            buildStep(title, completed) {

                return `

                    <div class="d-flex align-items-center mb-3">

                        <div class="me-3">

                            <i class="bi ${

                                completed

                                    ? "bi-check-circle-fill text-success"

                                    : "bi-circle text-secondary"

                            } fs-4"></i>

                        </div>

                        <div>

                            ${title}

                        </div>

                    </div>

                `;

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