window.CustomerInvoice = {

    invoices: [],

    async init() {

        document

            .getElementById(
                "refreshInvoiceButton"
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

            this.invoices =

                await CustomerPortalService
                    .getInvoices();

            this.render();

        }

        catch (error) {

            console.error(error);

            this.renderEmpty();

        }

        finally {

            CustomerApp.hideLoading();

        }

    },

    render() {

        const container =

            document.getElementById(

                "invoiceContainer"

            );

        if (!container) {

            return;

        }

        if (!this.invoices.length) {

            this.renderEmpty();

            return;

        }

        document.getElementById(

            "invoiceTotal"

        ).textContent =

            this.invoices.length;

        document.getElementById(

            "invoicePaid"

        ).textContent =

            this.invoices.filter(

                invoice =>

                    invoice.paymentStatus ===
                    "PAID"

            ).length;

        document.getElementById(

            "invoicePending"

        ).textContent =

            this.invoices.filter(

                invoice =>

                    invoice.paymentStatus !==
                    "PAID"

            ).length;

        const total =

            this.invoices.reduce(

                (sum, invoice) =>

                    sum +
                    Number(
                        invoice.grandTotal
                    ),

                0

            );

        document.getElementById(

            "invoiceAmount"

        ).textContent =

            this.formatCurrency(
                total
            );

        container.innerHTML =

            this.invoices.map(

                invoice => `

            <div class="col-xl-6 col-lg-6">

                <div class="customer-card invoice-card h-100">

                    <div class="invoice-header d-flex justify-content-between align-items-center">

                        <div class="d-flex align-items-center">

                            <div class="invoice-icon me-3">

                                <i class="bi bi-file-earmark-text fs-2 text-primary"></i>

                            </div>

                            <div>

                                <h5 class="mb-1">

                                    ${invoice.invoiceNumber}

                                </h5>

                                <small class="text-muted">

                                    Estimate

                                    ${invoice.estimateNumber}

                                </small>

                            </div>

                        </div>

                        <div>

                            ${this.invoiceBadge(
                                invoice.invoiceStatus
                            )}

                        </div>

                    </div>

                    <hr>

                    <div class="row">

                        <div class="col-6">

                            <small class="text-muted">

                                Invoice Status

                            </small>

                            <div class="fw-semibold">

                                ${this.formatStatus(
                                    invoice.invoiceStatus
                                )}

                            </div>

                        </div>

                        <div class="col-6">

                            <small class="text-muted">

                                Payment Status

                            </small>

                            <div>

                                ${this.paymentBadge(
                                    invoice.paymentStatus
                                )}

                            </div>

                        </div>

                    </div>

                    <hr>

                    <div class="row">

                        <div class="col-6">

                            <small class="text-muted">

                                Generated

                            </small>

                            <div>

                                ${this.formatDate(
                                    invoice.generatedAt
                                )}

                            </div>

                        </div>

                        <div class="col-6 text-end">

                            <small class="text-muted">

                                Grand Total

                            </small>

                            <h4 class="text-success mb-0">

                                ${this.formatCurrency(
                                    invoice.grandTotal
                                )}

                            </h4>

                        </div>

                    </div>

                    <div class="mt-4">

                        <button

                            class="btn btn-outline-primary w-100"

                            onclick="CustomerInvoice.download(${invoice.id})">

                            <i class="bi bi-download me-2"></i>

                            Download Invoice

                        </button>

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
                        "invoiceContainer"
                    );

                if (!container) {

                    return;

                }

                container.innerHTML = `

                    <div class="col-12">

                        <div class="customer-card text-center py-5">

                            <i class="bi bi-wallet2 display-3 text-secondary"></i>

                            <h3 class="mt-4">

                                No Invoices Available

                            </h3>

                            <p class="text-muted">

                                Your invoices will appear here once they are generated by the workshop.

                            </p>

                        </div>

                    </div>

                `;

            },

            async download(id) {

                try {

                    if (
                        InvoiceService &&
                        typeof InvoiceService.downloadInvoice === "function"
                    ) {

                        await InvoiceService.downloadInvoice(id);

                        return;

                    }

                    alert(
                        "Invoice download is not available yet."
                    );

                } catch (error) {

                    console.error(error);

                    alert(
                        "Unable to download invoice."
                    );

                }

            },

            invoiceBadge(status) {

                switch (status) {

                    case "GENERATED":

                        return `
                            <span class="badge bg-success">
                                Generated
                            </span>
                        `;

                    case "DRAFT":

                        return `
                            <span class="badge bg-warning text-dark">
                                Draft
                            </span>
                        `;

                    case "CANCELLED":

                        return `
                            <span class="badge bg-danger">
                                Cancelled
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

            paymentBadge(status) {

                switch (status) {

                    case "PAID":

                        return `
                            <span class="badge bg-success">
                                Paid
                            </span>
                        `;

                    case "PENDING":

                        return `
                            <span class="badge bg-warning text-dark">
                                Pending
                            </span>
                        `;

                    case "PARTIAL":

                        return `
                            <span class="badge bg-info">
                                Partial
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

                    .replace(
                        /\b\w/g,
                        c => c.toUpperCase()
                    );

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

            },

            formatDate(date) {

                if (!date) {

                    return "-";

                }

                return new Date(date)
                    .toLocaleString(
                        "en-IN",
                        {

                            day: "2-digit",

                            month: "short",

                            year: "numeric",

                            hour: "2-digit",

                            minute: "2-digit"

                        }
                    );

            }

        };