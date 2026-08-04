import { formatFileSizeInMb } from './formatFileSize'
import type { ResumeUploadResult } from './resumeUploadApi'

type UploadStatusPanelProps = {
  status: 'success' | 'error'
  result: ResumeUploadResult | null
  errorMessage: string | null
}

function UploadStatusPanel({ status, result, errorMessage }: UploadStatusPanelProps) {
  if (status === 'success' && result) {
    return (
      <div className="status-panel status-panel--success" role="status">
        <p className="status-panel__title">Resume uploaded successfully</p>
        <dl className="status-panel__details">
          <div className="status-panel__row">
            <dt>Generated ID</dt>
            <dd>{result.id}</dd>
          </div>
          <div className="status-panel__row">
            <dt>Original filename</dt>
            <dd>{result.originalFilename}</dd>
          </div>
          <div className="status-panel__row">
            <dt>File size</dt>
            <dd>{formatFileSizeInMb(result.fileSizeBytes)}</dd>
          </div>
          <div className="status-panel__row">
            <dt>Processing status</dt>
            <dd>{result.processingStatus}</dd>
          </div>
        </dl>
      </div>
    )
  }

  return (
    <div className="status-panel status-panel--error" role="alert">
      <p className="status-panel__title">Upload failed</p>
      <p>{errorMessage ?? 'Something went wrong. Please try again.'}</p>
    </div>
  )
}

export default UploadStatusPanel
