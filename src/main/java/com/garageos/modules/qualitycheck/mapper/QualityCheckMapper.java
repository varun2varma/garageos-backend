package com.garageos.modules.qualitycheck.mapper;

import com.garageos.modules.qualitycheck.dto.request.CreateQualityCheckRequest;
import com.garageos.modules.qualitycheck.dto.response.QualityCheckResponse;
import com.garageos.modules.qualitycheck.entity.QualityCheck;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface QualityCheckMapper {

//    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inspectedAt", ignore = true)
//    @Mapping(target = "createdAt", ignore = true)
//    @Mapping(target = "updatedAt", ignore = true)
    QualityCheck toEntity(CreateQualityCheckRequest request);

    @Mapping(target = "jobCardId", source = "jobCard.id")
    @Mapping(target = "jobCardNumber", source = "jobCard.jobCardNumber")
    QualityCheckResponse toResponse(QualityCheck entity);

    @BeanMapping(
            nullValuePropertyMappingStrategy =
                    NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "jobCard", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "inspectedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            CreateQualityCheckRequest request,
            @MappingTarget QualityCheck entity);

}