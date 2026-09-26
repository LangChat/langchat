<script setup lang="ts">
import { Compartment, EditorState, type Extension } from '@codemirror/state';
import { EditorView, placeholder, type ViewUpdate } from '@codemirror/view';
import { basicSetup } from 'codemirror';
import { css } from '@codemirror/lang-css';
import { html } from '@codemirror/lang-html';
import { javascript } from '@codemirror/lang-javascript';
import { json } from '@codemirror/lang-json';
import { markdown } from '@codemirror/lang-markdown';
import { sql } from '@codemirror/lang-sql';
import { yaml } from '@codemirror/lang-yaml';
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

interface Props {
  disabled?: boolean;
  height?: number | string;
  language?: string;
  minHeight?: number | string;
  placeholder?: string;
  readonly?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  height: 220,
  language: 'json',
  minHeight: 160,
  placeholder: '',
  readonly: false,
});

const modelValue = defineModel<null | string>('value', { default: '' });
const containerRef = ref<HTMLElement>();
let editorView: EditorView | null = null;
let internalUpdating = false;

const languageCompartment = new Compartment();
const readonlyCompartment = new Compartment();
const placeholderCompartment = new Compartment();

const isReadonly = computed(() => props.readonly || props.disabled);
const editorStyle = computed(() => ({
  height: normalizeSize(props.height),
  minHeight: normalizeSize(props.minHeight),
}));

function normalizeSize(value: number | string) {
  if (typeof value === 'number') {
    return `${value}px`;
  }
  if (/^\d+$/.test(value)) {
    return `${value}px`;
  }
  return value;
}

function resolveLanguageExtension(language: string): Extension {
  const lang = language.toLowerCase();
  if (lang === 'json' || lang === 'jsonc') {
    return json();
  }
  if (lang === 'js' || lang === 'javascript') {
    return javascript();
  }
  if (lang === 'ts' || lang === 'typescript') {
    return javascript({ typescript: true });
  }
  if (lang === 'html' || lang === 'xml') {
    return html();
  }
  if (lang === 'css' || lang === 'scss' || lang === 'less') {
    return css();
  }
  if (lang === 'md' || lang === 'markdown') {
    return markdown();
  }
  if (lang === 'sql' || lang === 'mysql' || lang === 'postgresql') {
    return sql();
  }
  if (lang === 'yaml' || lang === 'yml') {
    return yaml();
  }
  return [];
}

function resolvePlaceholderExtension(text: string): Extension {
  return text ? placeholder(text) : [];
}

function createEditor() {
  if (!containerRef.value) {
    return;
  }

  const state = EditorState.create({
    doc: modelValue.value || '',
    extensions: [
      basicSetup,
      EditorView.lineWrapping,
      languageCompartment.of(resolveLanguageExtension(props.language)),
      readonlyCompartment.of(EditorState.readOnly.of(isReadonly.value)),
      placeholderCompartment.of(resolvePlaceholderExtension(props.placeholder)),
      EditorView.updateListener.of((update: ViewUpdate) => {
        if (!update.docChanged || internalUpdating) {
          return;
        }
        modelValue.value = update.state.doc.toString();
      }),
      EditorView.theme({
        '&': {
          borderRadius: '8px',
          fontSize: '12px',
        },
        '.cm-content': {
          fontFamily:
            'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, Liberation Mono, Courier New, monospace',
          padding: '10px 12px',
        },
        '.cm-line': {
          padding: 0,
        },
        '.cm-gutters': {
          background: 'transparent',
          borderRight: '1px solid hsl(var(--border))',
          color: 'hsl(var(--muted-foreground))',
        },
        '.cm-activeLineGutter': {
          background: 'hsl(var(--muted) / 0.35)',
        },
      }),
    ],
  });

  editorView = new EditorView({
    parent: containerRef.value,
    state,
  });
}

function focus() {
  editorView?.focus();
}

defineExpose({ focus });

onMounted(createEditor);

onBeforeUnmount(() => {
  editorView?.destroy();
  editorView = null;
});

watch(
  () => modelValue.value,
  (value) => {
    if (!editorView) {
      return;
    }
    const next = value || '';
    const current = editorView.state.doc.toString();
    if (current === next) {
      return;
    }

    internalUpdating = true;
    editorView.dispatch({
      changes: {
        from: 0,
        insert: next,
        to: current.length,
      },
    });
    internalUpdating = false;
  },
);

watch(
  () => props.language,
  (language) => {
    if (!editorView) {
      return;
    }
    editorView.dispatch({
      effects: languageCompartment.reconfigure(
        resolveLanguageExtension(language),
      ),
    });
  },
);

watch(
  () => isReadonly.value,
  (value) => {
    if (!editorView) {
      return;
    }
    editorView.dispatch({
      effects: readonlyCompartment.reconfigure(EditorState.readOnly.of(value)),
    });
  },
);

watch(
  () => props.placeholder,
  (value) => {
    if (!editorView) {
      return;
    }
    editorView.dispatch({
      effects: placeholderCompartment.reconfigure(
        resolvePlaceholderExtension(value),
      ),
    });
  },
);
</script>

<template>
  <div class="lc-code-editor rounded-lg border border-border bg-background">
    <div ref="containerRef" :style="editorStyle"></div>
  </div>
</template>

<style scoped>
.lc-code-editor :deep(.cm-editor) {
  height: 100%;
}

.lc-code-editor :deep(.cm-scroller) {
  overflow: auto;
}

.lc-code-editor :deep(.cm-focused) {
  outline: 1px solid hsl(var(--primary) / 45%);
  outline-offset: -1px;
}
</style>
