package com.garageos.modules.media.mapper;

import com.garageos.modules.media.dto.response.JobCardMediaAuditResponse;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobCardMediaMapper {

    JobCardMediaResponse toResponse(JobCardMedia media);

    List<JobCardMediaResponse> toResponseList(List<JobCardMedia> media);

    /**
     * Customer-portal listing only — same shape as {@link #toResponse} but
     * excludes {@code uploadedBy} (internal employee user id) and
     * {@code lastError} (internal diagnostic text), neither of which a
     * customer needs. Employee-side callers keep using {@link #toResponse}/
     * {@link #toResponseList} unchanged. Named explicitly and referenced by
     * {@link #toCustomerResponseList} via {@code qualifiedByName} since
     * MapStruct would otherwise have two equally-valid JobCardMedia ->
     * JobCardMediaResponse methods (this one and {@link #toResponse}) to
     * choose from for the list mapping.
     */
    @Named("toCustomerResponse")
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "lastError", ignore = true)
    JobCardMediaResponse toCustomerResponse(JobCardMedia media);

    @IterableMapping(qualifiedByName = "toCustomerResponse")
    List<JobCardMediaResponse> toCustomerResponseList(List<JobCardMedia> media);

    /** Employee-side only — see {@link JobCardMediaAuditResponse}'s own doc comment for why this is a separate method/type. */
    @Mapping(target = "hasEvidenceImage", expression = "java(media.getEvidenceKey() != null)")
    JobCardMediaAuditResponse toAuditResponse(JobCardMedia media);

    List<JobCardMediaAuditResponse> toAuditResponseList(List<JobCardMedia> media);
}
