window.VehicleMasterService = {

    async getBrands() {
        return await Api.get("/vehicle-master/brands/dropdown");
    },

    async getModels(brandId) {
        return await Api.get("/vehicle-master/models/dropdown/brand/" + brandId);
    },

    async getVariants(modelId) {
        return await Api.get("/vehicle-master/variants/dropdown/model/" + modelId);
    },

    async getMetadata() {
            return await Api.get("/vehicle-master/variants/metadata");
    },

    async getTransmissionDropdown(modelId,variantId,fuelType){

        return await Api.get(
            `/vehicle-master/variants/${modelId}/variants/${variantId}/fuel-types/${fuelType}/transmissions`
        )
    },

    async getFuelTypeDropdown(modelId,variantId){

        return await Api.get(
            `/vehicle-master/variants/${modelId}/variants/${variantId}/fuel-types`
        )
    }

};