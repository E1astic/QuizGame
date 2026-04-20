# Инструкция по тестированию функционала игры-викторины

## Обзор архитектуры

Ваше приложение использует **WebSocket (STOMP)** для обмена сообщениями между клиентом и сервером.

### Конфигурация WebSocket:
- **Endpoint подключения**: `/ws-game`
- **Префикс для отправки сообщений**: `/app`
- **Топики для подписки**: `/topic`, `/queue`

---

## Доступные эндпоинты (STOMP destinations)

| Действие | Destination | Request Payload | Response |
|----------|-------------|-----------------|----------|
| Создать игру | `/app/game/create` | `{"quizId": "uuid"}` | `{"gameId": "uuid", "quizName": "string"}` |
| Присоединиться к игре | `/app/game/join` | `{"gameId": "uuid", "teamId": "uuid"}` | `{"gameId": "uuid", "teamId": "uuid", "success": boolean, "message": "string"}` |
| Начать игру | `/app/game/start` | `"uuid"` (gameId) | `{"gameId": "uuid", "success": boolean, "message": "string"}` |
| Отправить ответ | `/app/game/answer` | `{"gameId": "uuid", "questionId": "uuid", "answerId": "uuid", "teamId": "uuid"}` | `{"gameId": "uuid", "questionId": "uuid", "teamId": "uuid", "correct": boolean, "message": "string"}` |
| Завершить игру | `/app/game/finish` | `"uuid"` (gameId) | `{"gameId": "uuid", "success": boolean, "message": "string"}` |
| Получить вопросы | `/app/game/questions` | `"uuid"` (gameId) | `[{...QuestionAnswerDto}]` |

---

## Топики для подписки (получение уведомлений)

| Топик | Описание |
|-------|----------|
| `/topic/game/{gameId}/teams` | Уведомления о присоединении команд |
| `/topic/game/{gameId}/started` | Уведомление о начале игры |
| `/topic/game/{gameId}/questions` | Список вопросов для игры |
| `/topic/game/{gameId}/answer` | Ответы на вопросы |
| `/topic/game/{gameId}/finished` | Уведомление о завершении игры |

---

## JavaScript код для тестирования

Создайте файл `test-game.html` в корне проекта:

```html
<!DOCTYPE html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <title>Тестирование игры-викторины</title>
    <script src="https://cdn.jsdelivr.net/npm/@stomp/stompjs@7.0.0/bundles/stomp.umd.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
    <style>
        body { font-family: Arial, sans-serif; margin: 20px; }
        .section { margin: 20px 0; padding: 15px; border: 1px solid #ddd; }
        button { margin: 5px; padding: 10px 20px; cursor: pointer; }
        input, select { margin: 5px; padding: 8px; }
        #log { background: #f4f4f4; padding: 10px; height: 400px; overflow-y: scroll; font-family: monospace; }
        .success { color: green; }
        .error { color: red; }
        .info { color: blue; }
    </style>
</head>
<body>
    <h1>Тестирование игры-викторины</h1>

    <div class="section">
        <h2>1. Подключение к WebSocket</h2>
        <button onclick="connect()">Подключиться</button>
        <button onclick="disconnect()">Отключиться</button>
        <span id="connectionStatus">Статус: Не подключено</span>
    </div>

    <div class="section">
        <h2>2. Создание игры</h2>
        <input type="text" id="quizId" placeholder="Quiz ID (UUID)" value="00000000-0000-0000-0000-000000000001">
        <button onclick="createGame()">Создать игру</button>
    </div>

    <div class="section">
        <h2>3. Присоединение команды</h2>
        <input type="text" id="gameId" placeholder="Game ID (UUID)">
        <input type="text" id="teamId" placeholder="Team ID (UUID)" value="00000000-0000-0000-0000-000000000001">
        <button onclick="joinGame()">Присоединиться</button>
    </div>

    <div class="section">
        <h2>4. Управление игрой</h2>
        <button onclick="startGame()">Начать игру</button>
        <button onclick="getQuestions()">Получить вопросы</button>
        <button onclick="finishGame()">Завершить игру</button>
    </div>

    <div class="section">
        <h2>5. Отправка ответа</h2>
        <input type="text" id="questionId" placeholder="Question ID (UUID)">
        <input type="text" id="answerId" placeholder="Answer ID (UUID)">
        <button onclick="submitAnswer()">Отправить ответ</button>
    </div>

    <div class="section">
        <h2>Лог событий</h2>
        <button onclick="clearLog()">Очистить лог</button>
        <div id="log"></div>
    </div>

    <script>
        let stompClient = null;
        let currentGameId = null;

        function log(message, type = 'info') {
            const logDiv = document.getElementById('log');
            const timestamp = new Date().toLocaleTimeString();
            const className = type === 'error' ? 'error' : type === 'success' ? 'success' : 'info';
            logDiv.innerHTML += `<div class="${className}">[${timestamp}] ${message}</div>`;
            logDiv.scrollTop = logDiv.scrollHeight;
        }

        function clearLog() {
            document.getElementById('log').innerHTML = '';
        }

        function connect() {
            const socket = new SockJS('/ws-game');
            stompClient = Stomp.over(socket);

            stompClient.connect({},
                function(frame) {
                    log('Подключено к WebSocket: ' + frame, 'success');
                    document.getElementById('connectionStatus').innerText = 'Статус: Подключено';

                    // Подписка на общие уведомления
                    subscribeToGameTopics();
                },
                function(error) {
                    log('Ошибка подключения: ' + error, 'error');
                    document.getElementById('connectionStatus').innerText = 'Статус: Ошибка';
                }
            );
        }

        function disconnect() {
            if (stompClient !== null) {
                stompClient.disconnect();
                stompClient = null;
                log('Отключено от WebSocket', 'info');
                document.getElementById('connectionStatus').innerText = 'Статус: Не подключено';
            }
        }

        function subscribeToGameTopics() {
            if (!currentGameId) {
                log('Сначала создайте или укажите Game ID для подписки', 'error');
                return;
            }

            // Подписка на уведомления о командах
            stompClient.subscribe(`/topic/game/${currentGameId}/teams`, function(message) {
                log('Команды: ' + message.body, 'info');
            });

            // Подписка на начало игры
            stompClient.subscribe(`/topic/game/${currentGameId}/started`, function(message) {
                log('Игра началась: ' + message.body, 'success');
            });

            // Подписка на вопросы
            stompClient.subscribe(`/topic/game/${currentGameId}/questions`, function(message) {
                log('Вопросы получены: ' + message.body, 'success');
                try {
                    const questions = JSON.parse(message.body);
                    if (questions.length > 0) {
                        document.getElementById('questionId').value = questions[0].questionId;
                        if (questions[0].answers && questions[0].answers.length > 0) {
                            document.getElementById('answerId').value = questions[0].answers[0].answerId;
                        }
                        log('Первый вопрос и ответ автоматически заполнены', 'info');
                    }
                } catch(e) {
                    log('Ошибка парсинга вопросов: ' + e, 'error');
                }
            });

            // Подписка на ответы
            stompClient.subscribe(`/topic/game/${currentGameId}/answer`, function(message) {
                log('Ответ получен: ' + message.body, 'info');
            });

            // Подписка на завершение игры
            stompClient.subscribe(`/topic/game/${currentGameId}/finished`, function(message) {
                log('Игра завершена: ' + message.body, 'success');
            });

            log(`Подписан на все топики для игры ${currentGameId}`, 'info');
        }

        function send(destination, body) {
            if (stompClient && stompClient.connected) {
                log(`Отправка в ${destination}: ${JSON.stringify(body)}`, 'info');
                stompClient.send(destination, {}, JSON.stringify(body));
            } else {
                log('Не подключено к WebSocket', 'error');
            }
        }

        function createGame() {
            const quizId = document.getElementById('quizId').value;
            send('/app/game/create', { quizId: quizId });

            // Обработка ответа (через подписку на queue)
            stompClient.subscribe('/user/queue/response', function(message) {
                try {
                    const response = JSON.parse(message.body);
                    if (response.gameId) {
                        currentGameId = response.gameId;
                        document.getElementById('gameId').value = response.gameId;
                        log(`Игра создана: ${response.gameId}, Quiz: ${response.quizName}`, 'success');
                        subscribeToGameTopics();
                    }
                } catch(e) {}
            });
        }

        function joinGame() {
            const gameId = document.getElementById('gameId').value;
            const teamId = document.getElementById('teamId').value;

            if (!gameId || !teamId) {
                log('Укажите Game ID и Team ID', 'error');
                return;
            }

            send('/app/game/join', { gameId: gameId, teamId: teamId });
        }

        function startGame() {
            const gameId = document.getElementById('gameId').value;

            if (!gameId) {
                log('Укажите Game ID', 'error');
                return;
            }

            send('/app/game/start', gameId);
        }

        function getQuestions() {
            const gameId = document.getElementById('gameId').value;

            if (!gameId) {
                log('Укажите Game ID', 'error');
                return;
            }

            send('/app/game/questions', gameId);
        }

        function submitAnswer() {
            const gameId = document.getElementById('gameId').value;
            const questionId = document.getElementById('questionId').value;
            const answerId = document.getElementById('answerId').value;
            const teamId = document.getElementById('teamId').value;

            if (!gameId || !questionId || !answerId || !teamId) {
                log('Заполните все поля для ответа', 'error');
                return;
            }

            send('/app/game/answer', {
                gameId: gameId,
                questionId: questionId,
                answerId: answerId,
                teamId: teamId
            });
        }

        function finishGame() {
            const gameId = document.getElementById('gameId').value;

            if (!gameId) {
                log('Укажите Game ID', 'error');
                return;
            }

            send('/app/game/finish', gameId);
        }
    </script>
</body>
</html>
```

---

## Пошаговая инструкция для тестирования

### Шаг 1: Запуск приложения
```bash
cd /workspace/app
mvn spring-boot:run
```
Приложение должно запуститься на порту 8080 (или другом, указанном в application.properties).

### Шаг 2: Подготовка тестовых данных
Вам понадобятся UUID для:
- **Quiz ID** - ID викторины (должен существовать в базе данных)
- **Team ID** - ID команды (должен существовать в базе данных)

Проверьте наличие тестовых данных в базе или создайте их через API контента.

### Шаг 3: Открытие тестовой страницы
Откройте файл `test-game.html` в браузере.
**Важно**: Файл должен быть доступен через HTTP сервер (не как file://), чтобы работали WebSocket соединения.

Можно использовать простой HTTP сервер:
```bash
# Если установлен Python 3
python3 -m http.server 8000

# Или используйте npm http-server
npx http-server -p 8000
```

### Шаг 4: Последовательность действий для тестирования

#### 4.1 Подключение
1. Нажмите **"Подключиться"**
2. Проверьте лог: должно появиться сообщение "Подключено к WebSocket"

#### 4.2 Создание игры
1. Введите существующий **Quiz ID** (например, `00000000-0000-0000-0000-000000000001`)
2. Нажмите **"Создать игру"**
3. Проверьте лог: должно появиться сообщение с созданным Game ID
4. Game ID автоматически сохранится в поле

#### 4.3 Присоединение команды
1. Убедитесь, что Game ID заполнен
2. Введите **Team ID** (например, `00000000-0000-0000-0000-000000000001`)
3. Нажмите **"Присоединиться"**
4. Проверьте лог: должно появиться сообщение об успешном присоединении

#### 4.4 Начало игры
1. Нажмите **"Начать игру"**
2. Проверьте лог: должно появиться сообщение "Игра началась"
3. Вопросы должны автоматически загрузиться и отобразиться в логе
4. Первый Question ID и Answer ID должны автоматически заполниться в полях

#### 4.5 Отправка ответа
1. При необходимости скорректируйте Question ID и Answer ID
2. Нажмите **"Отправить ответ"**
3. Проверьте лог: должно появиться сообщение с результатом ответа (correct: true/false)

#### 4.6 Завершение игры
1. Нажмите **"Завершить игру"**
2. Проверьте лог: должно появиться сообщение "Игра завершена"

---

## Альтернативный способ: Тестирование через консоль браузера

Если вы хотите тестировать без HTML интерфейса, откройте консоль разработчика в браузере (F12) и выполните:

```javascript
// 1. Подключение
const socket = new SockJS('http://localhost:8080/ws-game');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);

    // 2. Подписка на топики
    stompClient.subscribe('/topic/game/test-game-id/teams', function(msg) {
        console.log('Teams:', msg.body);
    });

    stompClient.subscribe('/topic/game/test-game-id/started', function(msg) {
        console.log('Started:', msg.body);
    });

    stompClient.subscribe('/topic/game/test-game-id/questions', function(msg) {
        console.log('Questions:', msg.body);
    });

    stompClient.subscribe('/topic/game/test-game-id/answer', function(msg) {
        console.log('Answer:', msg.body);
    });

    stompClient.subscribe('/topic/game/test-game-id/finished', function(msg) {
        console.log('Finished:', msg.body);
    });

    // 3. Создание игры
    stompClient.send('/app/game/create', {}, JSON.stringify({
        quizId: '00000000-0000-0000-0000-000000000001'
    }));
});
```

---

## Проверка корректности работы

### Ожидаемые результаты:

1. **Создание игры**:
    - Возвращается GameCreateResponse с gameId и quizName
    - gameId генерируется сервером

2. **Присоединение команды**:
    - Возвращается GameJoinResponse с success=true
    - В топик `/topic/game/{gameId}/teams` отправляется уведомление

3. **Начало игры**:
    - Возвращается GameStartResponse с success=true
    - В топик `/topic/game/{gameId}/started` отправляется уведомление
    - В топик `/topic/game/{gameId}/questions` отправляется список вопросов

4. **Отправка ответа**:
    - Возвращается GameAnswerResponse с correct=true/false
    - В топик `/topic/game/{gameId}/answer` отправляется уведомление

5. **Завершение игры**:
    - Возвращается GameStartResponse с success=true
    - В топик `/topic/game/{gameId}/finished` отправляется уведомление

### Возможные ошибки:

- **400 Bad Request**: Неверный формат запроса или недостаточное количество команд
- **404 Not Found**: Игра или вопросы не найдены
- **IllegalStateException**: Попытка выполнить действие в неверном состоянии игры

---

## Примечания

1. Все UUID должны быть в формате: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
2. Для корректной работы необходимы существующие Quiz и Team в базе данных
3. WebSocket соединение требует, чтобы сервер был запущен
4. При тестировании через файл используйте HTTP сервер, а не protocol file://