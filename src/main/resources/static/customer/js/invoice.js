window.CustomerInvoice = {

    async init() {

        document
            .getElementById("refreshInvoiceButton")
            ?.addEventListener(

                "click",

                () => this.load()

            );

        await this.load();

    },

    async load() {

        try {

            CustomerApp.showLoading();

            if (!InvoiceService.getMyInvoices) {

                this.render([]);

                return;

            }

            const invoices =
                await InvoiceService.getMyInvoices();

            this.render(invoices);

        } catch (e) {

            console.error(e);

        } finally {

            CustomerApp.hideLoading();

        }

    },

    render(invoices) {

        const container =
            document.getElementById(
                "invoiceContainer"
            );

        if (!invoices.length) {

            container.innerHTML = `

                <div class="customer-card empty-state">

                    <i class="bi bi-file-earmark-text"></i>

                    <h5>

                        No Invoices

                    </h5>

                    <p>

                        No invoices available.

                    </p>

                </div>

            `;

            return;

        }

        container.innerHTML =
            invoices.map(invoice => `

                <div class="invoice-card">

                    <div class="invoice-header">

                        <div>

                            <div class="invoice-title">

                                ${invoice.invoiceNumber}

                            </div>

                            <div class="invoice-subtitle">

                                ${invoice.vehicleRegistrationNumber}

                            </div>

                        </div>

                        <span class="status-badge status-${invoice.paymentStatus.toLowerCase()}">

                            ${invoice.paymentStatus}

                        </span>

                    </div>

                    <div class="invoice-row">

                        <span class="invoice-label">

                            Job Card

                        </span>

                        <span class="invoice-value">

                            ${invoice.jobCardNumber}

                        </span>

                    </div>

                    <div class="invoice-row">

                        <span class="invoice-label">

                            Invoice Date

                        </span>

                        <span class="invoice-value">

                            ${invoice.invoiceDate}

                        </span>

                    </div>

                    <div class="invoice-row">

                        <span class="invoice-label">

                            Amount

                        </span>

                        <span class="invoice-value">

                            ₹ ${invoice.totalAmount}

                        </span>

                    </div>

                    <div class="invoice-footer">

                        <button
                                class="btn btn-outline-primary"
                                onclick="CustomerInvoice.download(${invoice.id})">

                            <i class="bi bi-download me-2"></i>

                            Download

                        </button>

                    </div>

                </div>

            `).join("");

    },

    async download(invoiceId) {

        try {

            if (!InvoiceService.downloadInvoice) {

                return;

            }

            await InvoiceService.downloadInvoice(

                invoiceId

            );

        } catch (e) {

            console.error(e);

        }

    }

};