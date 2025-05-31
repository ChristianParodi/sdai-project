export OLLAMA_NUM_PARALLEL=1
export OLLAMA_MAX_QUEUE=128
ollama serve &
ollama run llama3.2:3b