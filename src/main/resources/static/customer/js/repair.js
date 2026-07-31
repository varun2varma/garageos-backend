window.CustomerRepair = {

    async init() {

        document
            .getElementById("refreshRepairButton")
            ?.addEventListener(

                "click",

                () => this.loadRepairs()

            );

        await this.loadRepairs();

    },

    async loadRepairs() {

        try {

            CustomerApp.showLoading();

            if (!JobCardService.getMyJobCards) {

                this.render([]);

                return;

            }

            const jobs =
                await JobCardService.getMyJobCards();

            this.render(jobs);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(jobCards) {

        const container =
            document.getElementById(
                "repairTimeline"
            );

        if (!jobCards.length) {

            container.innerHTML = `

                <div class="customer-card empty-repair">

                    <i class="bi bi-tools"></i>

                    <h4>

                        No Active Repairs

                    </h4>

                    <p>

                        Repair jobs will appear here.

                    </p>

                </div>

            `;

            return;

        }

        container.innerHTML =
            jobCards.map(job => `

                <div class="repair-card">

                    <div class="repair-header">

                        <div>

                            <h4>

                                ${job.vehicleRegistrationNumber}

                            </h4>

                            <small>

                                ${job.jobCardNumber}

                            </small>

                        </div>

                        <span class="status-badge status-${job.status.toLowerCase()}">

                            ${job.status}

                        </span>

                    </div>

                    ${this.buildTimeline(job.status)}

                </div>

            `).join("");

    },

    buildTimeline(currentStatus) {

        const workflow = [

            "CREATED",

            "INSPECTION",

            "ESTIMATE_PENDING",

            "ESTIMATE_APPROVED",

            "REPAIR",

            "QUALITY_CHECK",

            "READY_FOR_DELIVERY",

            "DELIVERED"

        ];

        const currentIndex =
            workflow.indexOf(currentStatus);

        let html =
            `<div class="timeline">`;

        workflow.forEach((status, index) => {

            let css = "";

            if (index < currentIndex) {

                css = "completed";

            } else if (index === currentIndex) {

                css = "active";

            }

            html += `

                <div class="timeline-item ${css}">

                    <div class="timeline-dot"></div>

                    <div class="timeline-title">

                        ${this.formatStatus(status)}

                    </div>

                </div>

            `;

        });

        html += "</div>";

        return html;

    },

    formatStatus(status) {

        return status

            .replaceAll("_", " ")

            .toLowerCase()

            .replace(/\b\w/g, c => c.toUpperCase());

    }

};