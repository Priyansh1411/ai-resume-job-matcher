import type { ChangeEvent } from 'react'

const MAX_JOB_DESCRIPTION_CHARACTERS = 5000
export const MIN_JOB_DESCRIPTION_CHARACTERS = 100

type JobDescriptionCardProps = {
  value: string
  onChange: (value: string) => void
  isAnalyzeEnabled: boolean
  isUploading: boolean
  onAnalyzeClick: () => void
}

function JobDescriptionCard({
  value,
  onChange,
  isAnalyzeEnabled,
  isUploading,
  onAnalyzeClick,
}: JobDescriptionCardProps) {
  const nonWhitespaceLength = value.trim().length
  const showTooShortMessage =
    value.length > 0 && nonWhitespaceLength < MIN_JOB_DESCRIPTION_CHARACTERS

  function handleChange(event: ChangeEvent<HTMLTextAreaElement>) {
    onChange(event.target.value)
  }

  const describedBy = showTooShortMessage
    ? 'job-description-counter job-description-error'
    : 'job-description-counter'

  return (
    <div className="card">
      <h2>Add Job Description</h2>
      <label className="visually-hidden" htmlFor="job-description">
        Job description
      </label>
      <textarea
        id="job-description"
        className="job-textarea"
        placeholder="Paste the job description here, including required skills, responsibilities, and qualifications..."
        rows={10}
        maxLength={MAX_JOB_DESCRIPTION_CHARACTERS}
        value={value}
        onChange={handleChange}
        aria-describedby={describedBy}
      />

      <div className="job-textarea__footer">
        <span id="job-description-counter" className="char-counter">
          {value.length} / {MAX_JOB_DESCRIPTION_CHARACTERS}
        </span>
      </div>

      {showTooShortMessage && (
        <p id="job-description-error" className="form-error" role="alert">
          Please enter at least 100 characters.
        </p>
      )}

      <button
        type="button"
        className="btn btn--primary btn--full"
        disabled={!isAnalyzeEnabled || isUploading}
        onClick={onAnalyzeClick}
      >
        {isUploading ? 'Uploading Resume...' : 'Analyze Match'}
      </button>
    </div>
  )
}

export default JobDescriptionCard
