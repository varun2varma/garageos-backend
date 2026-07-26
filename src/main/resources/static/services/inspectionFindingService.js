const InspectionFindingService = {

    async loadRecommendations(vehicleId, odometer) {

        return await Api.post(

            "/inspection-findings/recommendations",

            {
                vehicleId,
                odometer
            }

        );

    }

};

window.InspectionFindingService = InspectionFindingService;