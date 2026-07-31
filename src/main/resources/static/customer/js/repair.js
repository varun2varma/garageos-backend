window.CustomerRepair = {

    jobCards: [],

    async init() {

        document
            .getElementById("refreshRepairButton")
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

            this.renderJobCards();

        } catch (e) {

            console.error(e);

            this.renderEmpty();

        } finally {

            CustomerApp.hideLoading();

        }

    },

    renderEmpty() {

        const container =
            document.getElementById(
                "repairTimeline"
            );

        if (!container) {

            return;

        }

        container.innerHTML = `

            <div class="customer-card text-center p-5">

                <i class="bi bi-tools display-4"></i>

                <h4 class="mt-3">

                    No Repair Jobs

                </h4>

                <p>

                    Repair tracking will appear here.

                </p>

            </div>

        `;

    },

    renderJobCards() {

        const container =
            document.getElementById(
                "repairTimeline"
            );

        if (!container) {

            return;

        }

        if (!this.jobCards.length) {

            this.renderEmpty();

            return;

        }

        document.getElementById("jobCardCount").textContent =
            this.jobCards.length;

        const delivered =
            this.jobCards.filter(job =>
                job.status === "DELIVERED"
            ).length;

        document.getElementById("completedRepairCount").textContent =
            delivered;

        const inProgress =
            this.jobCards.filter(job =>
                job.status !== "DELIVERED"
            ).length;

        document.getElementById("repairInProgressCount").textContent =
            inProgress;

        const pending =
            this.jobCards.filter(job =>
                job.status === "CREATED"
            ).length;

        document.getElementById("pendingRepairCount").textContent =
            pending;

        container.innerHTML =
            this.jobCards
                .map(job => `

                   `

                   <div class="col-lg-6">

                       <div class="customer-card h-100">

                           <div class="d-flex justify-content-between align-items-start">

                               <div>

                                   <h5>

                                       ${job.registrationNumber}

                                   </h5>

                                   <small class="text-muted">

                                       ${job.jobCardNumber}

                                   </small>

                               </div>

                               <span class="badge bg-primary">

                                   ${this.format(job.status)}

                               </span>

                           </div>

                           <hr>

                           <div class="mb-2">

                               <strong>Service Date</strong>

                               <div>

                                   ${job.serviceDate ?? "-"}

                               </div>

                           </div>

                           <div class="mb-3">

                               <strong>Estimated Delivery</strong>

                               <div>

                                   ${job.estimatedDeliveryDate ?? "-"}

                               </div>

                           </div>

                           <button
                               class="btn btn-primary w-100"

                               onclick="CustomerRepair.viewRepair('${job.jobCardNumber}')">

                               <i class="bi bi-search me-2"></i>

                               Track Repair

                           </button>

                       </div>

                   </div>

                `)
                .join("");

    },

    async viewRepair(jobCardNumber) {

        try {

            CustomerApp.showLoading();

            const repair =
                await CustomerPortalService
                    .getRepairTracking(jobCardNumber);

            this.renderTimeline(repair);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    renderTimeline(repair) {

        const container =
            document.getElementById(
                "repairTimeline"
            );

        container.innerHTML = `

            <div class="customer-card">

                <button
                    class="btn btn-link mb-3"

                    onclick="CustomerRepair.loadJobCards()">

                    ← Back

                </button>

                <h4>

                    ${repair.jobCardNumber}

                </h4>

                <div class="mb-3 text-muted">

                    ${repair.registrationNumber}

                </div>

                ${this.step(
                    "Inspection",
                    repair.inspectionCompleted
                )}

                ${this.step(
                    "Estimate Prepared",
                    repair.estimatePrepared
                )}

                ${this.step(
                    "Estimate Approved",
                    repair.estimateApproved
                )}

                ${this.step(
                    "Repair Completed",
                    repair.repairCompleted
                )}

                ${this.step(
                    "Quality Checked",
                    repair.qualityChecked
                )}

                ${this.step(
                    "Invoice Generated",
                    repair.invoiceGenerated
                )}

                ${this.step(
                    "Payment Completed",
                    repair.paymentCompleted
                )}

                <hr>

                <div class="d-flex justify-content-between">

                    <span>

                        Current Status

                    </span>

                    <strong>

                        ${this.format(repair.status)}

                    </strong>

                </div>

                <div class="d-flex justify-content-between mt-2">

                    <span>

                        Estimated Delivery

                    </span>

                    <strong>

                        ${repair.estimatedDeliveryDate ?? "-"}

                    </strong>

                </div>

            </div>

        `;

    },

    step(title, completed) {

        return `

            <div class="d-flex align-items-center mb-3">

                <div class="me-3">

                    ${completed
                        ? '<i class="bi bi-check-circle-fill text-success fs-5"></i>'
                        : '<i class="bi bi-circle text-secondary fs-5"></i>'}

                </div>

                <div>

                    ${title}

                </div>

            </div>

        `;

    },

    format(status) {

        return status
            ?.replaceAll("_", " ")
            .toLowerCase()
            .replace(/\b\w/g, c => c.toUpperCase());

    }

};