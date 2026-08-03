window.InvoiceService = {

    async createInvoice(request) {

        return await Api.post(
            "/invoices",
            request
        );

    },

    async getInvoice(id) {

        return await Api.get(
            `/invoices/${id}`
        );

    }

};