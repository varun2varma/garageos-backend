window.Stepper = {

    steps: [

        {
            id: "search",
            title: "Search",
            icon: "bi-search",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR",
                "TECHNICIAN",
                "ACCOUNTANT",
                "CASHIER"
            ],
            component: "SearchStep"
        },

        {
            id: "job",
            title: "Job Card",
            icon: "bi-card-checklist",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ],
            component: "JobStep"
        },

        {
            id: "inspection",
            title: "Inspection",
            icon: "bi-clipboard-check",
            roles: [
                "MANAGER",
                "TECHNICIAN"
            ],
            component: "InspectionStep"
        },

        {
            id: "estimate",
            title: "Estimate",
            icon: "bi-cash-stack",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ],
            component: "EstimateStep"
        },

        {
            id: "estimateItems",
            title: "Estimate Items",
            icon: "bi-list-check",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ],
            component: "EstimateItemStep"
        },

        {
            id: "estimateSummary",
            title: "Estimate Summary",
            icon: "bi-file-earmark-text",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ],
            component: "EstimateSummaryStep"
        },

        {
            id: "approval",
            title: "Approval",
            icon: "bi-check-circle",
            roles: [
                "MANAGER"
            ],
            component: "ApprovalStep"
        },

        {
            id: "repair",
            title: "Repair",
            icon: "bi-tools",
            roles: [
                "MANAGER",
                "TECHNICIAN"
            ],
            component: "RepairStep"
        },

        {
            id: "quality",
            title: "Quality Check",
            icon: "bi-shield-check",
            roles: [
                "MANAGER",
                "TECHNICIAN"
            ],
            component: "QualityCheckStep"
        },

        {
            id: "invoice",
            title: "Invoice",
            icon: "bi-receipt",
            roles: [
                "MANAGER",
                "ACCOUNTANT"
            ],
            component: "InvoiceStep"
        },

        {
            id: "payment",
            title: "Payment",
            icon: "bi-credit-card",
            roles: [
                "MANAGER",
                "ACCOUNTANT",
                "CASHIER"
            ],
            component: "PaymentStep"
        },

        {
            id: "delivery",
            title: "Delivery",
            icon: "bi-truck",
            roles: [
                "MANAGER",
                "SERVICE_ADVISOR"
            ],
            component: "DeliveryStep"
        }

    ],

    getVisibleSteps() {

        return this.steps.filter(step =>

            step.roles.some(role =>

                Permissions.has(role)

            )

        );

    },

    getStep(index) {

        return this.getVisibleSteps()[index];

    },

    getCurrentStep(currentStep) {

        return this.getVisibleSteps()[currentStep - 1];

    },

    getTotalSteps() {

        return this.getVisibleSteps().length;

    },

    render(currentStep) {

        const steps = this.getVisibleSteps();

        return `

    <div class="card shadow-sm border-0">

        <div class="card-body">

            <div class="d-flex justify-content-between flex-wrap">

                ${steps.map((step, index) => {

                    const completed =

                        index + 1 < currentStep;

                    const active =

                        index + 1 === currentStep;

                    return `

    <div class="text-center flex-fill">

        <div

            class="rounded-circle mx-auto mb-2

            d-flex align-items-center justify-content-center

            ${completed
                ? "bg-success"
                : active
                    ? "bg-primary"
                    : "bg-secondary"}

            text-white"

            style="width:42px;height:42px;">

            ${completed

                ? '<i class="bi bi-check-lg"></i>'

                : `<i class="bi ${step.icon}"></i>`}

        </div>

        <small>

            ${step.title}

        </small>

    </div>

    `;

                }).join("")}

            </div>

        </div>

    </div>

    `;

    }

};