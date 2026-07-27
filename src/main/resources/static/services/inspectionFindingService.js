const InspectionFindingService = {

    async loadRecommendations(request) {

        return await Api.post(

            "/inspection-findings/recommendations",

            request

        );

    }

};

window.InspectionFindingService = InspectionFindingService;