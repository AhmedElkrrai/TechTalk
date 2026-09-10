package com.elkrrai.techtalk.utils

/** Literal prompt text + a sample JSON schema, shown on the Get More Content screen so
 * users can ask an LLM to author tip packs. */
fun buildTipsPrompt(): String = """
    Write a TechTalk tip pack as JSON, following this exact schema:

    {
      "version": 1,
      "author": "your name",
      "description": "short description of this pack",
      "entries": [
        {
          "technologyName": "Kotlin",
          "topicName": "Coroutines",
          "topicDescription": "Structured concurrency in Kotlin",
          "tips": [
            {
              "title": "Short, punchy title",
              "content": "1-3 sentences explaining the concept clearly.",
              "codeSnippet": "optional code example, or omit this field",
              "codeLang": "kotlin",
              "difficulty": "BEGINNER"
            }
          ]
        }
      ]
    }

    Rules:
    - "technologyName" must be one of: Kotlin, Android, Swift, iOS, Go.
    - "difficulty" must be one of: BEGINNER, INTERMEDIATE, ADVANCED.
    - Each tip should teach exactly one focused idea.
    - Keep "content" concise — this renders as a single feed card.
""".trimIndent()

/** Prompt template for battle (quiz question) packs, matching [com.elkrrai.techtalk
 * .domain.model.battle.BattlePack]'s schema. */
fun buildBattlePrompt(): String = """
    Write a TechTalk battle pack as JSON, following this exact schema:

    {
      "version": 1,
      "author": "your name",
      "description": "short description of this pack",
      "entries": [
        {
          "technologyName": "Kotlin",
          "topicName": "Coroutines",
          "topicDescription": "Structured concurrency in Kotlin",
          "questions": [
            {
              "questionText": "What does `suspend` mean on a function?",
              "difficulty": "BEGINNER",
              "explanation": "One or two sentences explaining the correct answer.",
              "answers": [
                { "answerText": "Correct answer", "isCorrect": true },
                { "answerText": "Wrong answer", "isCorrect": false },
                { "answerText": "Wrong answer", "isCorrect": false },
                { "answerText": "Wrong answer", "isCorrect": false }
              ]
            }
          ]
        }
      ]
    }

    Rules:
    - "technologyName" must be one of: Kotlin, Android, Swift, iOS, Go.
    - "difficulty" must be one of: BEGINNER, INTERMEDIATE, ADVANCED.
    - Each question needs exactly 4 answers, with exactly one "isCorrect": true.
    - "technologyName" and "topicName" should match an existing tip pack's topic where possible.
""".trimIndent()
