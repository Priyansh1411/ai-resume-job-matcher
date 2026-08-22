import type { ResumeMatchResult } from './resumeMatchApi'

type MatchResultPanelProps = {
  status: 'loading' | 'success' | 'error'
  result: ResumeMatchResult | null
  errorMessage: string | null
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

        <div className="match-skill-group">
          <p className="match-skill-group__label">Matched skills</p>
          <div className="skill-chip-list">
            {result.matchedSkills.length > 0 ? (
              result.matchedSkills.map((skill) => (
                <span className="skill-chip" key={skill}>
                  {skill}
                </span>
              ))
            ) : (
              <span className="dropzone__hint">No matched skills</span>
            )}
          </div>
        </div>

        <div className="match-skill-group">
          <p className="match-skill-group__label">Missing skills</p>
          <div className="skill-chip-list">
            {result.missingSkills.length > 0 ? (
              result.missingSkills.map((skill) => (
                <span className="skill-chip skill-chip--missing" key={skill}>
                  {skill}
                </span>
              ))
            ) : (
              <span className="dropzone__hint">No missing skills</span>
            )}
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
