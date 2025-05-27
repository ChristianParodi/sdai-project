import requests
import json


def send_request_to_ollama(prompt):
    url = "http://localhost:11434/api/generate"

    payload = {
        "model": "llama3.2:8b",
        "prompt": prompt,
        "stream": False
    }

    headers = {
        "Content-Type": "application/json"
    }

    try:
        response = requests.post(url, headers=headers,
                                 data=json.dumps(payload))
        response.raise_for_status()   # Raises HTTPError for bad responses
        data = response.json()
        return data
    except requests.exceptions.RequestException as e:
        print("An error occurred:", e)
        return None


if __name__ == "__main__":
    prompt_text = "Hello, how are you?"
    result = send_request_to_ollama(prompt_text)
    if result:
        print("Response from ollama:")
        print(json.dumps(result, indent=2))