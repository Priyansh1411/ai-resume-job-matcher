package com.resumematcher.backend.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "resume_skills")
public class ResumeSkill {

	@Id
	@Column(name = "id", length = 36, updatable = false)
	private String id;

	@Column(name = "resume_id", nullable = false, length = 36, updatable = false)
	private String resumeId;

	@Column(name = "skill_name", nullable = false, length = 100)
	private String skillName;

	public ResumeSkill() {
	}

	public ResumeSkill(String resumeId, String skillName) {
		this.resumeId = resumeId;
		this.skillName = skillName;
	}

	@PrePersist
	protected void prePersist() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getResumeId() {
		return resumeId;
	}

	public void setResumeId(String resumeId) {
		this.resumeId = resumeId;
	}

	public String getSkillName() {
		return skillName;
	}

	public void setSkillName(String skillName) {
		this.skillName = skillName;
	}

}
