import type { ResumeAnalysisResult } from './resumeAnalysisApi'

type AiAnalysisPanelProps = {
  status: 'loading' | 'success' | 'error'
  result: ResumeAnalysisResult | null
  errorMessage: string | null
}

type AnalysisListProps = {
  items: string[]
  emptyMessage: string
}

function AnalysisList({ items, emptyMessage }: AnalysisListProps) {
  if (items.length === 0) {
    return <span className="dropzone__hint">{emptyMessage}</span>
  }

  return (
    <ul>
      {items.map((item) => (
        <li key={item}>{item}</li>
      ))}
    </ul>
  )
}

function AiAnalysisPanel({ status, result, errorMessage }: AiAnalysisPanelProps) {
  if (status === 'loading') {
    return (
      <div className="status-panel" role="status">
        <p className="status-panel__title">Generating AI analysis...</p>
      </div>
    )
  }

  if (status === 'success' && result) {
    return (
      <div className="status-panel status-panel--success" role="status">
        <p className="status-panel__title">AI Analysis</p>

        <div className="match-skill-section">
          <h3 className="match-skill-section__title">Strengths</h3>
          <AnalysisList items={result.strengths} emptyMessage="No strengths identified" />
        </div>

        <div className="match-skill-section">
          <h3 className="match-skill-section__title">Gaps</h3>
          <AnalysisList items={result.gaps} emptyMessage="No gaps identified" />
        </div>

        <div className="match-skill-section">
          <h3 className="match-skill-section__title">Suggestions</h3>
          <AnalysisList items={result.suggestions} emptyMessage="No suggestions available" />
        </div>
      </div>
    )
  }

  return (
    <div className="status-panel status-panel--error" role="alert">
      <p className="status-panel__title">Could not generate AI analysis</p>
      <p>{errorMessage ?? 'Something went wrong. Please try again.'}</p>
    </div>
  )
}

export default AiAnalysisPanel
