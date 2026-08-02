window.InvoiceService = {

    async createInvoice(request) {

        return await Api.post(

            "/invoices",

            request

        );

    }

};