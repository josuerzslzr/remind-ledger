{{- define "remind-ledger.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{- define "remind-ledger.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- include "remind-ledger.name" . }}
{{- end }}
{{- end }}

{{- define "remind-ledger.labels" -}}
app.kubernetes.io/name: {{ include "remind-ledger.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
