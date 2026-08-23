import { useState } from 'react'
import ResumeUploadCard from './ResumeUploadCard'
import JobDescriptionCard, { MIN_JOB_DESCRIPTION_CHARACTERS } from './JobDescriptionCard'
import UploadStatusPanel from './UploadStatusPanel'
import ResumeProfilePanel from './ResumeProfilePanel'
import MatchResultPanel from './MatchResultPanel'
import { ResumeUploadError, uploadResume } from './resumeUploadApi'
import type { ResumeUploadResult } from './resumeUploadApi'
import { ResumeProfileError, fetchResumeProfile } from './resumeProfileApi'
import type { ResumeProfileResult } from './resumeProfileApi'
import { ResumeMatchError, fetchResumeMatch } from './resumeMatchApi'
import type { ResumeMatchResult } from './resumeMatchApi'
import AiAnalysisPanel from './AiAnalysisPanel'
import { ResumeAnalysisError, fetchResumeAnalysis } from './resumeAnalysisApi'
import type { ResumeAnalysisResult } from './resumeAnalysisApi'
import './App.css'

type UploadStatus = 'idle' | 'uploading' | 'success' | 'error'
type ProfileStatus = 'idle' | 'loading' | 'success' | 'error'
type MatchStatus = 'idle' | 'loading' | 'success' | 'error'
type AnalysisStatus = 'idle' | 'loading' | 'success' | 'error'

function App() {
  const [resumeFile, setResumeFile] = useState<File | null>(null)
  const [jobDescription, setJobDescription] = useState('')
  const [uploadStatus, setUploadStatus] = useState<UploadStatus>('idle')
  const [uploadResult, setUploadResult] = useState<ResumeUploadResult | null>(null)
  const [uploadErrorMessage, setUploadErrorMessage] = useState<string | null>(null)
  const [profileStatus, setProfileStatus] = useState<ProfileStatus>('idle')
  const [profileResult, setProfileResult] = useState<ResumeProfileResult | null>(null)
  const [profileErrorMessage, setProfileErrorMessage] = useState<string | null>(null)
  const [matchStatus, setMatchStatus] = useState<MatchStatus>('idle')
  const [matchResult, setMatchResult] = useState<ResumeMatchResult | null>(null)
  const [matchErrorMessage, setMatchErrorMessage] = useState<string | null>(null)
  const [analysisStatus, setAnalysisStatus] = useState<AnalysisStatus>('idle')
  const [analysisResult, setAnalysisResult] = useState<ResumeAnalysisResult | null>(null)
  const [analysisErrorMessage, setAnalysisErrorMessage] = useState<string | null>(null)

  const isResumeValid = resumeFile !== null
  const isJobDescriptionValid =
    jobDescription.trim().length >= MIN_JOB_DESCRIPTION_CHARACTERS
  const isAnalyzeEnabled = isResumeValid && isJobDescriptionValid

  function handleResumeFileSelected(file: File | null) {
    setResumeFile(file)
    setUploadStatus('idle')
    setUploadResult(null)
    setUploadErrorMessage(null)
    setProfileStatus('idle')
    setProfileResult(null)
    setProfileErrorMessage(null)
    setMatchStatus('idle')
    setMatchResult(null)
    setMatchErrorMessage(null)
    setAnalysisStatus('idle')
    setAnalysisResult(null)
    setAnalysisErrorMessage(null)
  }

  async function handleAnalyzeClick() {
    if (!isAnalyzeEnabled || uploadStatus === 'uploading' || !resumeFile) {
      return
    }

    setUploadStatus('uploading')
    setUploadErrorMessage(null)
    setProfileStatus('idle')
    setProfileResult(null)
    setProfileErrorMessage(null)
    setMatchStatus('idle')
    setMatchResult(null)
    setMatchErrorMessage(null)
    setAnalysisStatus('idle')
    setAnalysisResult(null)
    setAnalysisErrorMessage(null)

    try {
      const result = await uploadResume(resumeFile)
      setUploadResult(result)
      setUploadStatus('success')

      setProfileStatus('loading')
      try {
        const profile = await fetchResumeProfile(result.id)
        setProfileResult(profile)
        setProfileStatus('success')
      } catch (profileErr) {
        const profileMessage =
          profileErr instanceof ResumeProfileError
            ? profileErr.message
            : 'Could not load the resume profile.'
        setProfileErrorMessage(profileMessage)
        setProfileStatus('error')
      }

      setMatchStatus('loading')
      try {
        const match = await fetchResumeMatch(result.id, jobDescription)
        setMatchResult(match)
        setMatchStatus('success')
      } catch (matchErr) {
        const matchMessage =
          matchErr instanceof ResumeMatchError
            ? matchErr.message
            : 'Could not calculate the match score.'
        setMatchErrorMessage(matchMessage)
        setMatchStatus('error')
      }
    } catch (err) {
      const message =
        err instanceof ResumeUploadError ? err.message : 'Upload failed. Please try again.'
      setUploadErrorMessage(message)
      setUploadStatus('error')
    }
  }

  async function handleGenerateAnalysisClick() {
    if (!uploadResult || analysisStatus === 'loading') {
      return
    }

    setAnalysisStatus('loading')
    setAnalysisErrorMessage(null)

    try {
      const analysis = await fetchResumeAnalysis(uploadResult.id, jobDescription)
      setAnalysisResult(analysis)
      setAnalysisStatus('success')
    } catch (analysisErr) {
      const analysisMessage =
        analysisErr instanceof ResumeAnalysisError
          ? analysisErr.message
          : 'Could not generate the AI analysis.'
      setAnalysisErrorMessage(analysisMessage)
      setAnalysisStatus('error')
    }
  }

  return (
    <div className="page">
      <header className="navbar">
        <div className="navbar__inner">
          <a className="brand" href="#top">
            <span className="brand__name">ResumeMatch AI</span>
            <span className="badge">AI Powered</span>
          </a>
          <nav className="nav-links" aria-label="Main navigation">
            <a href="#top">Home</a>
            <a href="#how-it-works">How It Works</a>
          </nav>
          <a className="btn btn--primary" href="#upload">
            Get Started
          </a>
        </div>
      </header>

      <main>
        <section className="hero" id="top">
          <h1>Match Your Resume to the Right Job</h1>
          <p className="hero__description">
            Upload your resume and paste a job description to instantly see
            how well they align, so you can tailor your application before
            you apply.
          </p>
          <div className="feature-pills">
            <span className="pill">Skill Match Analysis</span>
            <span className="pill">Missing Keyword Detection</span>
          </div>
        </section>

        <section
          className="workspace"
          id="upload"
          aria-label="Resume and job description input"
        >
          <ResumeUploadCard onFileSelected={handleResumeFileSelected} />

          <JobDescriptionCard
            value={jobDescription}
            onChange={setJobDescription}
            isAnalyzeEnabled={isAnalyzeEnabled}
            isUploading={uploadStatus === 'uploading'}
            onAnalyzeClick={handleAnalyzeClick}
          />
        </section>

        {(uploadStatus === 'success' || uploadStatus === 'error') && (
          <div className="status-panel-wrapper">
            <UploadStatusPanel
              status={uploadStatus}
              result={uploadResult}
              errorMessage={uploadErrorMessage}
            />
          </div>
        )}

        {profileStatus !== 'idle' && (
          <div className="status-panel-wrapper">
            <ResumeProfilePanel
              status={profileStatus}
              result={profileResult}
              errorMessage={profileErrorMessage}
            />
          </div>
        )}

        {matchStatus !== 'idle' && (
          <div className="status-panel-wrapper">
            <MatchResultPanel
              status={matchStatus}
              result={matchResult}
              errorMessage={matchErrorMessage}
            />
          </div>
        )}

        {matchStatus === 'success' && (
          <div className="status-panel-wrapper">
            <button
              type="button"
              className="btn btn--secondary"
              disabled={analysisStatus === 'loading'}
              onClick={handleGenerateAnalysisClick}
            >
              {analysisStatus === 'loading' ? 'Generating AI Analysis...' : 'Generate AI Analysis'}
            </button>
          </div>
        )}

        {analysisStatus !== 'idle' && (
          <div className="status-panel-wrapper">
            <AiAnalysisPanel
              status={analysisStatus}
              result={analysisResult}
              errorMessage={analysisErrorMessage}
            />
          </div>
        )}

        <section
          className="how-it-works"
          id="how-it-works"
          aria-label="How it works"
        >
          <h2 className="section-title">How It Works</h2>
          <ol className="steps">
            <li className="step">
              <span className="step__number" aria-hidden="true">1</span>
              <h3>Upload Resume</h3>
              <p>Add your resume in PDF or DOCX format.</p>
            </li>
            <li className="step">
              <span className="step__number" aria-hidden="true">2</span>
              <h3>Add Job Description</h3>
              <p>Paste the job posting you are applying for.</p>
            </li>
            <li className="step">
              <span className="step__number" aria-hidden="true">3</span>
              <h3>View Match Results</h3>
              <p>See your compatibility score and suggestions.</p>
            </li>
          </ol>
        </section>
      </main>
    </div>
  )
}

export default App
