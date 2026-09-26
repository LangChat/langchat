import { onScopeDispose, ref } from 'vue';

/**
 * 浏览器 SpeechRecognition 的最小结构类型(W3C 规范尚未进入 TS DOM lib)。
 */
interface SpeechRecognitionAlternativeLike {
  transcript: string;
}

interface SpeechRecognitionResultLike {
  0: SpeechRecognitionAlternativeLike;
  isFinal: boolean;
  length: number;
}

interface SpeechRecognitionEventLike {
  resultIndex: number;
  results: ArrayLike<SpeechRecognitionResultLike>;
}

interface SpeechRecognitionLike {
  abort(): void;
  addEventListener(
    type: 'end',
    listener: () => void,
  ): void;
  addEventListener(
    type: 'error',
    listener: (event: { error?: string }) => void,
  ): void;
  addEventListener(
    type: 'result',
    listener: (event: SpeechRecognitionEventLike) => void,
  ): void;
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  start(): void;
  stop(): void;
}

type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

function resolveRecognitionConstructor(): null | SpeechRecognitionConstructor {
  const scope = window as unknown as {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };
  return scope.SpeechRecognition ?? scope.webkitSpeechRecognition ?? null;
}

/** 格式化录音计时展示(m:ss)。 */
export function formatRecTime(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, '0')}`;
}

interface VoiceInputOptions {
  /** 识别语言,默认中文 */
  lang?: string;
  /** 识别结果回调(含中间结果),text 为本次录音的完整转写文本 */
  onTranscript?: (text: string) => void;
}

/**
 * 浏览器语音输入:基于 Web Speech API,不依赖后端接口。
 * start() 后实时把转写文本通过 onTranscript 抛出;
 * stop() 保留已转写文本,cancel() 丢弃本次转写。
 * 不支持的浏览器(如 Firefox)会返回 supported = false,由调用方降级隐藏入口。
 */
export function useVoiceInput(options: VoiceInputOptions = {}) {
  const RecognitionConstructor = resolveRecognitionConstructor();
  const supported = Boolean(RecognitionConstructor);

  const recording = ref(false);
  const seconds = ref(0);
  const transcript = ref('');

  let recognition: null | SpeechRecognitionLike = null;
  let timer: null | ReturnType<typeof setInterval> = null;
  let stoppedManually = false;

  function ensureRecognition() {
    if (!RecognitionConstructor) {
      return null;
    }
    if (recognition) {
      return recognition;
    }
    const instance = new RecognitionConstructor();
    instance.continuous = true;
    instance.interimResults = true;
    instance.lang = options.lang ?? 'zh-CN';
    instance.addEventListener('result', (event) => {
      let text = '';
      for (let i = 0; i < event.results.length; i += 1) {
        text += event.results[i]?.[0]?.transcript ?? '';
      }
      transcript.value = text.trim();
      options.onTranscript?.(transcript.value);
    });
    instance.addEventListener('error', () => {
      // 权限被拒、无网络等错误统一交给 end 事件收尾
    });
    instance.addEventListener('end', () => {
      if (!stoppedManually) {
        // Chrome 静音超时等场景会自动结束,同步收尾计时状态
        stopTimer();
        recording.value = false;
      }
    });
    recognition = instance;
    return recognition;
  }

  function startTimer() {
    seconds.value = 0;
    timer = setInterval(() => {
      seconds.value += 1;
    }, 1000);
  }

  function stopTimer() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  /** 开始听写。 */
  function start() {
    const instance = ensureRecognition();
    if (!instance || recording.value) {
      return;
    }
    stoppedManually = false;
    transcript.value = '';
    try {
      instance.start();
      recording.value = true;
      startTimer();
    } catch {
      // 重复 start() 等场景浏览器会抛 InvalidStateError,忽略即可
    }
  }

  /** 停止听写,保留已转写文本。 */
  function stop() {
    if (!recording.value || !recognition) {
      return;
    }
    stoppedManually = true;
    stopTimer();
    recording.value = false;
    recognition.stop();
  }

  /** 取消听写,丢弃本次转写文本。 */
  function cancel() {
    if (!recognition) {
      return;
    }
    stoppedManually = true;
    stopTimer();
    recording.value = false;
    transcript.value = '';
    recognition.abort();
  }

  onScopeDispose(() => {
    stopTimer();
    recognition?.abort();
  });

  return { cancel, recording, seconds, start, stop, supported, transcript };
}
