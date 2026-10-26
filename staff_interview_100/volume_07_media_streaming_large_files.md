# Volume 7: Streaming Media, Audio/Video & Large File Pipelines (Q61 - Q70)
*Targeting Staff / Principal Full-Stack Interviews (Mercor, micro1, FAANG)*
*Candidate Context: Balasubramanian Shanmugham (AWS S3, Presigned Multipart, SQS, Media Streaming, RAG PDF Document Ingestion)*

---

## Question 61: Zero-Server-Memory 2GB Media Uploads via S3 Presigned Multipart
**Interviewer:** *"If 500 users upload 2GB video/audio files simultaneously through a Spring Boot or Node.js server, the server crashes with OutOfMemoryError and saturates network bandwidth. How do you design an asynchronous, resilient media upload pipeline on AWS with zero server memory footprint?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Streaming multi-gigabyte media uploads directly through application web servers is an anti-pattern: it ties up HTTP threads for minutes, exhausts memory buffers, and wastes server bandwidth on raw byte transit.
>
> The production solution is **Direct-to-S3 Multipart Upload using AWS Presigned URLs**:
>
> 1. **Initiation**: The client calls our API with file metadata (size, MIME type, checksum). The backend verifies authentication and tenant quotas, generates an S3 Multipart Upload ID, and returns a batch of presigned URLs for each 10MB chunk.
> 2. **Client Upload**: The client uploads chunks in parallel directly to AWS S3. Not a single byte of the 2GB media passes through our application servers.
> 3. **Completion**: Upon finishing all chunks, the client calls `CompleteMultipartUpload`. S3 stitches the file together and fires an `s3:ObjectCreated:*` notification to an AWS SQS queue.
> 4. **Asynchronous Processing**: A fleet of containerized background workers on ECS/Kubernetes picks up the SQS message. The worker streams the file using FFmpeg to generate adaptive bitrate HLS chunks (.m3u8) for video or extracts audio tracks for transcription.
>
> To defend against orphaned partial uploads that waste cloud storage costs, we configure an S3 Lifecycle Rule to automatically abort incomplete multipart uploads after 7 days."

### Working TypeScript / AWS SDK Code
```typescript
import { S3Client, CreateMultipartUploadCommand, UploadPartCommand } from '@aws-sdk/client-s3';
import { getSignedUrl } from '@aws-sdk/s3-request-presigner';

const s3 = new S3Client({ region: 'us-east-1' });

export async function generateMultipartUploadUrls(bucket: string, key: string, partsCount: number) {
  const createRes = await s3.send(new CreateMultipartUploadCommand({
    Bucket: bucket,
    Key: key,
    ContentType: 'video/mp4'
  }));

  const uploadId = createRes.UploadId;
  const presignedUrls: { partNumber: number; url: string }[] = [];

  for (let i = 1; i <= partsCount; i++) {
    const command = new UploadPartCommand({
      Bucket: bucket,
      Key: key,
      UploadId: uploadId,
      PartNumber: i,
    });

    const url = await getSignedUrl(s3, command, { expiresIn: 3600 });
    presignedUrls.push({ partNumber: i, url });
  }

  return { uploadId, presignedUrls };
}
```

---

## Question 62: Resumable Chunked Uploads: Managing Part ETags & Retries
**Interviewer:** *"During a 2GB file upload from a mobile browser on a cellular connection, the network drops at Part 85 of 100. How does the client manage part ETags, resume without re-uploading the first 84 parts, and assemble the final file?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "In high-scale media pipelines, mobile networks disconnect frequently. Forcing a user to restart a 2GB upload from Part 1 after 85 parts have completed results in severe bandwidth waste and terrible user experience.
>
> We implement **Resumable Client-Side Chunking with Persistent State Tracking**:
>
> 1. **Client Part Chunking**: The browser slices the `File` object using `file.slice(start, end)` into equal 10MB chunks.
> 2. **State Storage in IndexedDB**: For every part uploaded, AWS S3 returns an `ETag` HTTP response header representing the MD5 checksum of that chunk. The client saves this in IndexedDB:
>    `{ uploadId, partNumber: 85, eTag: '"d41d8cd98f00b204e9800998ecf8427e"' }`
> 3. **Resume Protocol**: When connectivity restores:
>    - The client calls the backend: `GET /api/media/parts?uploadId=xyz`.
>    - The backend queries S3 via `ListPartsCommand` and returns the list of parts already received by S3.
>    - The client diffs the required parts against the received parts and resumes uploading *only* the missing parts (Part 86 to 100).
> 4. **Assembly**: Once all 100 parts are uploaded, the client calls `CompleteMultipartUploadCommand`, passing the sorted array of `{ PartNumber, ETag }`. S3 validates the checksums, concatenates the chunks internally on disk, and makes the file available immediately."

---

## Question 63: Asynchronous Video Transcoding with S3 Events, SQS & FFmpeg
**Interviewer:** *"Once a 2GB video is uploaded to S3, how do you design an event-driven worker pipeline using S3 Event Notifications, SQS, and containerized FFmpeg workers to transcode the video into 1080p, 720p, and 480p without server bottlenecks?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Video transcoding is an intensely CPU and GPU-bound operation. Performing transcoding inside web API pods will starve API threads and cause cluster instability.
>
> We decouple transcoding using an **Asynchronous S3 Event-Driven Worker Pipeline**:
>
> 1. **Event Ingestion**: When the multipart upload completes in S3, S3 emits an `s3:ObjectCreated:CompleteMultipartUpload` notification. This event routes directly to an **AWS SQS Queue** with Server-Side Encryption.
> 2. **Worker Fleet (AWS ECS / Kubernetes Pods)**: A pool of autoscaling containerized workers running Node.js/Python and native FFmpeg poll the SQS queue.
> 3. **Streaming Processing**:
>    - The worker retrieves the message, sets an initial SQS visibility timeout of 10 minutes, and downloads the raw source video.
>    - FFmpeg executes multi-resolution adaptive encoding:
>      `ffmpeg -i input.mp4 -vf scale=w=1920:h=1080 -b:v 5M 1080p.mp4 -vf scale=w=1280:h=720 -b:v 2.5M 720p.mp4 ...`
>    - While FFmpeg processes, a background heartbeat thread periodically extends the SQS message visibility timeout (`ChangeMessageVisibility`) every 2 minutes so other workers don't pick up the in-flight job.
> 4. **Delivery**: Transcoded files and thumbnails are uploaded to a public CDN S3 bucket, a `VideoTranscodedEvent` is published to Kafka, and the SQS message is deleted."

---

## Question 64: Generating Adaptive Bitrate Video Streaming Manifests (HLS)
**Interviewer:** *"Explain how HTTP Live Streaming (HLS) works. What is the role of the master `.m3u8` playlist, media playlist, and `.ts` chunk files? How does the video player dynamically switch between 480p and 1080p when bandwidth changes?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Traditional MP4 progressive streaming requires downloading a massive contiguous file. If a mobile user's network speed drops, the video player freezes because it cannot dynamically request a lower quality stream for the next frame.
>
> **HTTP Live Streaming (HLS)** solves this by segmenting video into short, independent 2-to-6 second video chunks (`.ts` or `.m4s` fragments) across multiple bitrate renditions:
>
> 1. **The Master Playlist (`master.m3u8`)**: The player first downloads the Master Playlist. This file contains metadata describing all available stream qualities:
>    ```
>    #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
>    1080p/index.m3u8
>    #EXT-X-STREAM-INF:BANDWIDTH=1500000,RESOLUTION=854x480
>    480p/index.m3u8
>    ```
> 2. **Media Playlists**: Each individual resolution has its own playlist listing sequential chunk files:
>    `segment_001.ts`, `segment_002.ts`, etc.
> 3. **Adaptive Bitrate Algorithm (ABR)**:
>    While the player renders `segment_001.ts`, it measures the download throughput of that chunk. If network throughput drops below 2 Mbps, the player's internal ABR engine seamlessly switches its next chunk request from `1080p/segment_002.ts` to `480p/segment_002.ts`.
>
> Because all chunk streams are strictly time-aligned at identical keyframe (GOP) intervals, the user experiences continuous playback without stuttering or buffering."

---

## Question 65: Audio Chunking & Distributed Speech Transcription Pipelines
**Interviewer:** *"You have 10,000 recorded phone calls from Morgan Stanley CRM that need speech-to-text transcription. Passing a 1-hour audio file directly to AI APIs fails due to payload limits and rate limits. How do you chunk audio files and manage distributed worker rate limiting?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Transcribing thousands of long-form audio calls against AI APIs (like OpenAI Whisper or AWS Transcribe) presents two major challenges: audio payload size limits (typically 25MB) and strict third-party rate limits (e.g. 50 requests per minute).
>
> We resolve this using **Silence-Aware Audio Chunking and Distributed Leaky Bucket Queues**:
>
> 1. **Silence-Aware Slicing via FFmpeg**: Rather than blindly splitting audio every 10 minutes (which cuts off words mid-syllable), we run an FFmpeg silence detection filter (`silencedetect=noise=-30dB:d=0.5`). FFmpeg identifies conversational pauses and splits the audio into 10-minute segments at natural sentence boundaries.
> 2. **Distributed Rate Limiting via Redis Token Bucket**:
>    To avoid getting rate-limited (HTTP 429), our worker fleet synchronizes against a central Redis Token Bucket:
>    Before submitting a chunk to the speech API, the worker must acquire an execution token from Redis (`redis.rpop('whisper_tokens')`).
> 3. **Map-Reduce Transcript Assembly**:
>    Each chunk is transcribed in parallel with its temporal offset. When all chunks for Call 101 finish, a reducer concatenates the text segments chronologically, recalculates timestamp offsets, and writes the complete transcript to PostgreSQL."

---

## Question 66: Handling SQS Dead-Letter Queues (DLQ) & Visibility Timeouts
**Interviewer:** *"In your media processing queue, a corrupted video file causes FFmpeg to segfault, crashing the worker immediately. SQS retries the message indefinitely, crashing every worker in the fleet. How do you configure visibility timeouts, redrive policies, and Dead-Letter Queues?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "This failure mode is known as a **Poison-Pill Message**. When a corrupted file causes an unhandled worker crash or segfault, the worker process dies without acknowledging the SQS message.
>
> After the **Visibility Timeout** expires, SQS makes the message visible again. Another worker picks it up, crashes, and the cycle repeats in an endless loop, taking down the entire worker fleet.
>
> We defend against this by configuring an **SQS Redrive Policy with a Dead-Letter Queue (DLQ)**:
>
> 1. **Visibility Timeout Tuning**: We set `VisibilityTimeout = 600` (10 minutes), matching the maximum expected duration of a heavy transcode job.
> 2. **Max Receive Count**: We set `maxReceiveCount = 3`. If a message fails or crashes workers 3 times, SQS stops routing it to the main queue and moves it to the `media-transcoding-dlq`.
> 3. **CloudWatch Alarms & Poison Isolation**:
>    A CloudWatch Alarm monitors `ApproximateNumberOfMessagesVisible` on the DLQ. If DLQ depth exceeds 0, an alert fires to PagerDuty.
> 4. **Inspection and Remediation**:
>    Engineers inspect the DLQ message metadata and quarantine the corrupted video file. Once bugs are fixed, we use AWS SQS Redrive to replay safe messages back into the main queue."

---

## Question 67: Protecting S3 Media Assets with CloudFront Signed Cookies
**Interviewer:** *"A customer purchases access to a premium train route documentary video. If you serve media via public S3 URLs, users share the link on forums. Signed URLs require re-signing every individual HLS chunk (`.ts`). How do CloudFront Signed Cookies protect multi-chunk HLS streams?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When streaming HLS video, a single 1-hour video consists of a master playlist, several media playlists, and hundreds of `.ts` video chunk segments.
>
> Using **CloudFront Signed URLs** is impractical for HLS: every single `.ts` URL inside the `.m3u8` playlist would need to be pre-signed with query parameters, breaking standard video player caching and complicating playlist generation.
>
> The production standard is **CloudFront Signed Cookies**:
>
> 1. When the user purchases access, the authentication server validates their entitlement and issues three cryptographic HTTP cookies signed by CloudFront's private key:
>    - `CloudFront-Policy`: Specifies the allowed resource path pattern (`/videos/premium_101/*`) and expiration timestamp.
>    - `CloudFront-Signature`: Cryptographic RSA signature of the policy.
>    - `CloudFront-Key-Pair-Id`: Public key identifier.
> 2. The cookies are set with `HttpOnly`, `Secure`, and `SameSite=None` attributes.
> 3. When the video player requests `/videos/premium_101/master.m3u8` and subsequent chunks (`segment_01.ts`, `segment_02.ts`), the browser automatically attaches the signed cookies to every GET request.
> 4. CloudFront Edge nodes verify the signature locally at the edge in microseconds. If an unauthenticated user copies a `.ts` chunk link and pastes it in an incognito window, the request lacks the cookie and CloudFront returns HTTP 403 Forbidden."

---

## Question 68: HTML5 Video Player Buffering & Bandwidth Telemetry in React
**Interviewer:** *"In a Next.js video portal, how do you build an instrumented video player that tracks user buffer starvation, video stalls, and playback bitrate to emit Quality of Experience (QoE) telemetry to PostHog or Datadog?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Tracking video Quality of Experience (QoE) is critical for streaming platforms to identify CDN latency spikes and client-side playback stalls before users churn.
>
> We instrument the HTML5 `<video>` element using native Media Events in React:
>
> 1. **Buffer Health Monitoring**: By inspecting `videoElement.buffered`, we calculate buffer length:
>    $$\text{BufferAhead} = \text{buffered.end}(\text{current}) - \text{video.currentTime}$$
>    If buffer ahead drops below 2 seconds, we flag imminent starvation.
> 2. **Stall & Rebuffer Detection**:
>    - We listen to the `waiting` and `stalled` events: this fires when playback stops because the next frame has not downloaded. We record the stall start timestamp.
>    - When the `playing` event fires, we compute stall duration: `now - stallStart`.
> 3. **QoE Telemetry Pipeline**:
>    Every 10 seconds, or immediately upon a stall, a lightweight telemetry beacon is emitted containing:
>    `{ videoId, currentBitrate, stallCount, totalStallDurationMs, bufferLengthSec, droppedFrames }`.
>    We collect dropped frames via `videoElement.getVideoPlaybackQuality().droppedVideoFrames`.
>
> These metrics stream to our PostHog/Prometheus analytics dashboard to evaluate CDN performance across geographical regions."

---

## Question 69: High-Volume PDF Document Ingestion (RAG Q&A Pipeline)
**Interviewer:** *"On your resume, you built a production RAG document Q&A agent for PDFs with event-driven orchestration, step-level observability, and per-user rate limiting. When a user uploads a 500-page scanned PDF, text extraction locks CPU threads for 30 seconds. How do you design an asynchronous, chunked document ingestion pipeline?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "Parsing a 500-page PDF document synchronously inside an API thread will trigger an HTTP 504 gateway timeout and consume 100% of the pod's CPU, starving other users.
>
> We architected our RAG Document Ingestion pipeline as an **Asynchronous Event-Driven Pipeline with Step-Level Observability**:
>
> 1. **Document Upload**: The PDF is uploaded directly to S3. The API creates a `document_job` record with status `PROCESSING` and returns a job ID to the client immediately.
> 2. **Distributed Page Slicing**: A worker picks up the job from an SQS queue. Using Apache PDFBox or PyMuPDF, it splits the 500-page document into 25-page chunks.
> 3. **Parallel Text Extraction & OCR**: The 20 chunk tasks are dispatched in parallel across background worker threads. Each chunk extracts text, generates vector embeddings via OpenAI or HuggingFace embeddings models, and writes vectors to PostgreSQL `pgvector`.
> 4. **Step-Level Observability**: Every step (Parsing, Chunking, Embedding, Indexing) logs progress percentages to a Redis Hash: `job:progress:{jobId}`. The React frontend polls this status via TanStack Query to show a real-time progress bar.
> 5. **Per-User Concurrency Throttling**: To prevent a single user from uploading 50 large PDFs and starving the cluster, we enforce a distributed semaphore in Redis: a user cannot have more than 2 active document ingestion jobs running concurrently."

---

## Question 70: Eliminating S3 Cloud Storage Leaks from Incomplete Uploads
**Interviewer:** *"When clients initiate multipart uploads to AWS S3 but abandon them midway due to network failures or closing tabs, S3 permanently stores those uploaded chunks and charges for them every month. How do you automate the cleanup of incomplete multipart uploads?"*

### 🎙️ Spoken Verbal Response (1.5 - 2 Minutes)
> "When using S3 Multipart Upload, each uploaded part is stored as an independent object block in S3 until `CompleteMultipartUpload` is called. If a client uploads 90 out of 100 parts of a 2GB file and crashes, those 1.8GB of chunks sit in S3 indefinitely.
>
> They do not show up when running standard `s3 ls` commands because the file was never completed. However, AWS bills for every single byte of those hidden chunks every month. In high-volume systems with millions of uploads, abandoned multipart uploads can silently generate thousands of dollars in wasted cloud spend.
>
> We eliminate this by configuring an **S3 Bucket Lifecycle Configuration Rule**:
>
> ```xml
> <Rule>
>     <ID>AutoAbortIncompleteMultipartUploads</ID>
>     <Status>Enabled</Status>
>     <Filter><Prefix></Prefix></Filter>
>     <AbortIncompleteMultipartUpload>
>         <DaysAfterInitiation>7</DaysAfterInitiation>
>     </AbortIncompleteMultipartUpload>
> </Rule>
> ```
>
> This declarative policy instructs AWS S3 to automatically scan the bucket and permanently delete all orphaned multipart chunks exactly 7 days after the upload was initiated, maintaining 100% cloud cost hygiene without writing custom cleanup cron jobs."
