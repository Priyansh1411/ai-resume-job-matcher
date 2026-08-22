import type { ResumeProfileResult } from './resumeProfileApi'

type ResumeProfilePanelProps = {
  status: 'loading' | 'success' | 'error'
  result: ResumeProfileResult | null
  errorMessage: string | null
}

function ResumeProfilePanel({ status, result, errorMessage }: ResumeProfilePanelProps) {
  if (status === 'loading') {
    return (
      <div className="status-panel" role="status">
        <p className="status-panel__title">Loading resume profile...</p>
      </div>
    )
  }

  if (status === 'success' && result) {
    return (
      <div className="status-panel status-panel--success" role="status">
        <p className="status-panel__title">Resume Profile</p>
        <dl className="status-panel__details">
          <div className="status-panel__row">
            <dt>Name</dt>
            <dd>{result.fullName ?? 'Not detected'}</dd>
          </div>
          <div className="status-panel__row">
            <dt>Email</dt>
            <dd>{result.email ?? 'Not detected'}</dd>
          </div>
          <div className="status-panel__row">
            <dt>Phone</dt>
            <dd>{result.phone ?? 'Not detected'}</dd>
          </div>
          <div className="status-panel__row">
            <dt>Profile status</dt>
            <dd>{result.profileStatus}</dd>
          </div>
        </dl>

        <div className="skill-chip-list">
          {result.skills.length > 0 ? (
            result.skills.map((skill) => (
              <span className="skill-chip" key={skill}>
                {skill}
              </span>
            ))
          ) : (
            <span className="dropzone__hint">No skills detected</span>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="status-panel status-panel--error" role="alert">
      <p className="status-panel__title">Could not load resume profile</p>
      <p>{errorMessage ?? 'Something went wrong. Please try again.'}</p>
    </div>
  )
}

export default ResumeProfilePanel
