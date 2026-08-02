window.InspectionService = {

    async createInspection(
        complaintId,
        request
    ) {

        return await Api.post(

            `/complaints/${complaintId}/inspections`,

            request

        );

    }

};