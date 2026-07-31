window.CustomerEstimate = {

    async init() {

        document
            .getElementById("refreshEstimateButton")
            ?.addEventListener(

                "click",

                () => this.load()

            );

        await this.load();

    },

    async load() {

        try {

            CustomerApp.showLoading();

            if (!EstimateService.getPendingEstimates) {

                this.render([]);

                return;

            }

            const estimates =
                await EstimateService.getPendingEstimates();

            this.render(estimates);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(estimates) {

        const container =
            document.getElementById("estimateContainer");

        if (!estimates.length) {

            container.innerHTML = `

                <div class="customer-card empty-state">

                    <i class="bi bi-receipt"></i>

                    <h5>

                        No Pending Estimates

                    </h5>

                    <p>

                        You don't have any estimates awaiting approval.

                    </p>

                </div>

            `;

            return;

        }

        container.innerHTML =
            estimates.map(estimate => `

                <div class="estimate-card">

                    <div class="estimate-header">

                        <h5>

                            ${estimate.jobCardNumber}

                        </h5>

                        <small>

                            ${estimate.vehicleRegistrationNumber}

                        </small>

                    </div>

                    <div class="estimate-body">

                        ${estimate.items.map(item => `

                            <div class="estimate-item">

                                <div>

                                    <div class="estimate-item-name">

                                        ${item.name}

                                    </div>

                                    <div class="estimate-item-description">

                                        Qty : ${item.quantity}

                                    </div>

                                </div>

                                <div class="estimate-price">

                                    ₹ ${item.totalPrice}

                                </div>

                            </div>

                        `).join("")}

                        <div class="estimate-total">

                            <span>

                                Total

                            </span>

                            <span>

                                ₹ ${estimate.totalAmount}

                            </span>

                        </div>

                        <div class="estimate-actions">

                            <button
                                    class="btn btn-outline-danger"
                                    onclick="CustomerEstimate.reject(${estimate.id})">

                                Reject

                            </button>

                            <button
                                    class="btn btn-primary"
                                    onclick="CustomerEstimate.approve(${estimate.id})">

                                Approve

                            </button>

                        </div>

                    </div>

                </div>

            `).join("");

    },

    async approve(id) {

        try {

            CustomerApp.showLoading();

            if (EstimateService.approveEstimate) {

                await EstimateService.approveEstimate(id);

            }

            await this.load();

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    async reject(id) {

        try {

            CustomerApp.showLoading();

            if (EstimateService.rejectEstimate) {

                await EstimateService.rejectEstimate(id);

            }

            await this.load();

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    }

};