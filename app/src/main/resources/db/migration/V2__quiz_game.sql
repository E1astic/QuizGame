-- РЕГИСТРАЦИЯ КОМАНД НА КВИЗ
create table if not exists quizzes_to_teams
(
    quiz_id   uuid        not null references quizzes (id) on delete cascade,
    team_id   uuid        not null references teams (id) on delete cascade,
    joined_at timestamptz not null default now(),

    primary key (quiz_id, team_id)
);

-- ИГРОВОЙ ПРОЦЕСС
create table if not exists quiz_games
(
    id              uuid primary key default gen_random_uuid(),
    quiz_id         uuid        not null references quizzes (id),
    status          varchar(50) not null default 'WAITING' check (status in ('WAITING', 'IN_PROGRESS', 'FINISHED')),
    current_question_index int not null default 0,
    started_at      timestamptz,
    finished_at     timestamptz
);

create table if not exists quiz_game_teams
(
    game_id uuid not null references quiz_games (id) on delete cascade,
    team_id uuid not null references teams (id) on delete cascade,
    score   int  not null default 0,

    primary key (game_id, team_id)
);

create table if not exists quiz_game_answers
(
    id        uuid primary key default gen_random_uuid(),
    game_id   uuid        not null references quiz_games (id) on delete cascade,
    team_id   uuid        not null references teams (id) on delete cascade,
    question_id uuid      not null references questions (id),
    answer_id   uuid      not null references answers (id),
    is_correct bool       not null,
    answered_at timestamptz not null default now()
);
