package com.garageos.modules.media.mapper;

import com.garageos.modules.media.dto.response.JobCardMediaAuditResponse;
import com.garageos.modules.media.dto.response.JobCardMediaResponse;
import com.garageos.modules.media.entity.JobCardMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JobCardMediaMapper {

    JobCardMediaResponse toResponse(JobCardMedia media);

    List<JobCardMediaResponse> toResponseList(List<JobCardMedia> media);

    /** Employee-side only — see {@link JobCardMediaAuditResponse}'s own doc comment for why this is a separate method/type. */
    @Mapping(target = "hasEvidenceImage", expression = "java(media.getEvidenceKey() != null)")
    JobCardMediaAuditResponse toAuditResponse(JobCardMedia media);

    List<JobCardMediaAuditResponse> toAuditResponseList(List<JobCardMedia> media);
}
