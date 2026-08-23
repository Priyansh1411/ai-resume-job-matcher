import type { ResumeMatchResult } from './resumeMatchApi'

type MatchResultPanelProps = {
  status: 'loading' | 'success' | 'error'
  result: ResumeMatchResult | null
  errorMessage: string | null
}

type SkillChipListProps = {
  skills: string[]
  missing?: boolean
  emptyMessage: string
}

function SkillChipList({ skills, missing, emptyMessage }: SkillChipListProps) {
  if (skills.length === 0) {
    return <span className="dropzone__hint">{emptyMessage}</span>
  }

  return (
    <div className="skill-chip-list">
      {skills.map((skill) => (
        <span className={missing ? 'skill-chip skill-chip--missing' : 'skill-chip'} key={skill}>
          {skill}
        </span>
      ))}
    </div>
  )
}

function MatchResultPanel({ status, result, errorMessage }: MatchResultPanelProps) {
  if (status === 'loading') {
    return (
      <div className="status-panel" role="status">
        <p className="status-panel__title">Calculating match score...</p>
      </div>
    )
  }

  if (status === 'success' && result) {
    return (
      <div className="status-panel status-panel--success" role="status">
        <p className="status-panel__title">Match Results</p>
        <p className="match-score">{result.matchScorePercentage}% match</p>

        <div className="match-skill-section">
          <h3 className="match-skill-section__title">Required Skills</h3>

          <div className="match-skill-group">
            <p className="match-skill-group__label">Matched</p>
            <SkillChipList
              skills={result.matchedRequiredSkills}
              emptyMessage="No required skills matched"
            />
          </div>

          <div className="match-skill-group">
            <p className="match-skill-group__label">Missing</p>
            <SkillChipList
              skills={result.missingRequiredSkills}
              missing
              emptyMessage="No required skills missing"
            />
          </div>
        </div>

        <div className="match-skill-section">
          <h3 className="match-skill-section__title">Preferred Skills</h3>

          <div className="match-skill-group">
            <p className="match-skill-group__label">Matched</p>
            <SkillChipList
              skills={result.matchedPreferredSkills}
              emptyMessage="No preferred skills matched"
            />
          </div>

          <div className="match-skill-group">
            <p className="match-skill-group__label">Missing</p>
            <SkillChipList
              skills={result.missingPreferredSkills}
              missing
              emptyMessage="No preferred skills missing"
            />
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="status-panel status-panel--error" role="alert">
      <p className="status-panel__title">Could not calculate match</p>
      <p>{errorMessage ?? 'Something went wrong. Please try again.'}</p>
    </div>
  )
}

export default MatchResultPanel
