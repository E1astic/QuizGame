-- ПОЛЬЗОВАТЕЛИ
create table if not exists users
(
    id         uuid primary key      default gen_random_uuid(),
    role       varchar(50)  not null check (role in ('MODERATOR', 'PLAYER')),
    email      varchar(255) not null unique,
    password   varchar      not null,
    phone      varchar(50)  not null,
    name       varchar(255) not null,
    surname    varchar(255),
    created_at timestamptz  not null default now()
);
insert into users(role, email, password, phone, name, surname)
values ('MODERATOR', 'moder1@mail.ru', '$2a$10$sD7MxYsW4rklyMdL6VzRIuD4thGZe.z5uR1AmBvmxN81g7.AFDhdK', '+7111111',
        'moder1', 'moder1');
insert into users(id, role, email, password, phone, name, surname)
values ('dd8cf789-e82d-4005-8aa5-dea4395c6b05', 'PLAYER', 'user1@mail.ru',
        '$2a$10$c4m80MbkSmim.lkp/scrGOJGDDeU4JTGSUs9Z0PbOCBs/RAzVZ4GO', '+7111111223344', 'user1', 'user1');
insert into users(id, role, email, password, phone, name, surname)
values ('fb9ea154-4665-4d48-a211-7389388f9532', 'PLAYER', 'user2@mail.ru',
        '$2a$10$ytzSoFnrgk4CeqlBKsjGEOPLpoMCJGaa9E4B5splVQ.UB.exVrHjy', '+7111111223344', 'user2', 'user2');


-- ИГРОКИ И КОМАНДЫ
create table if not exists players
(
    user_id      uuid primary key references users (id) on delete cascade,
    nickname     varchar(50)   not null unique,
    games_played int           not null default 0,
    wins         int           not null default 0,
    rating       decimal(4, 3) not null default 0
);
insert into players(user_id, nickname, games_played, wins, rating)
values ('dd8cf789-e82d-4005-8aa5-dea4395c6b05', 'Крушитель_черепов_1', 0, 0, 0.000);
insert into players(user_id, nickname, games_played, wins, rating)
values ('fb9ea154-4665-4d48-a211-7389388f9532', 'Крушитель_черепов_2', 0, 0, 0.000);


create table if not exists teams
(
    id           uuid primary key       default gen_random_uuid(),
    name         varchar(100)  not null unique,
    max_size     int           not null default 5,
    games_played int           not null default 0,
    wins         int           not null default 0,
    rating       decimal(4, 3) not null default 0,
    created_at   timestamptz   not null default now()
);
insert into teams(id, name, max_size, games_played, wins, rating, created_at)
values ('980a5cdc-0df9-4175-9966-7fe3432b9978', 'Ледяные_короли', 5, 0, 0, 0.000, now());

create table if not exists players_to_teams
(
    player_id  uuid references players (user_id) on delete cascade,
    team_id    uuid references teams (id) on delete cascade,
    is_capitan bool        not null default false,
    joined_at  timestamptz not null default now(),

    primary key (player_id, team_id)
);
insert into players_to_teams(player_id, team_id, is_capitan, joined_at)
values ('dd8cf789-e82d-4005-8aa5-dea4395c6b05', '980a5cdc-0df9-4175-9966-7fe3432b9978', true, now());
insert into players_to_teams(player_id, team_id, is_capitan, joined_at)
values ('fb9ea154-4665-4d48-a211-7389388f9532', '980a5cdc-0df9-4175-9966-7fe3432b9978', false, now());


-- ПРИГЛАШЕНИЯ В КОМАНДУ
create table if not exists team_invitations
(
    id           uuid primary key     default gen_random_uuid(),
    sender_id    uuid        not null,
    recipient_id uuid        not null,
    team_id      uuid        not null,
    created_at   timestamptz not null default now()
);


-- КОНТЕНТ КВИЗА
create table if not exists topics
(
    id   uuid primary key default gen_random_uuid(),
    name varchar(100) not null unique
);
insert into topics(id, name)
values ('f58662f1-313b-4c96-9348-0f46259992f8', 'Музыка');
insert into topics(id, name)
values ('09eeb4dc-e792-414a-b32d-dc1c12554b0d', 'Компьютерные игры');

create table if not exists questions
(
    id         uuid primary key default gen_random_uuid(),
    name       varchar     not null,
    difficulty varchar(50) not null,
    topic_id   uuid        not null references topics (id) on delete cascade
);
insert into questions(id, name, difficulty, topic_id)
values ('e5672044-90ca-4604-9c32-35037271f84e', 'Фраза ...Здесь так красиво я перестаю дышать... из песни', 'EASY',
        'f58662f1-313b-4c96-9348-0f46259992f8');
insert into questions(id, name, difficulty, topic_id)
values ('38727c7c-8dda-44f3-9f23-b7b9e1d307ec', 'Фраза ...Ты моя скрипка - я Антонио Вивальди... из песни', 'MEDIUM',
        'f58662f1-313b-4c96-9348-0f46259992f8');

insert into questions(id, name, difficulty, topic_id)
values ('460d531b-2de3-4190-9d9d-99744e18acff', 'В какой серии игр есть персонаж Капитан Прайс?', 'EASY',
        '09eeb4dc-e792-414a-b32d-dc1c12554b0d');
insert into questions(id, name, difficulty, topic_id)
values ('648becaf-2f17-40c0-8a5f-b5e0eccae36b', 'В какой игре есть персонаж Эцио Аудиторе?', 'MEDIUM',
        '09eeb4dc-e792-414a-b32d-dc1c12554b0d');


create table if not exists answers
(
    id          uuid primary key default gen_random_uuid(),
    name        varchar not null,
    is_correct  bool    not null,
    question_id uuid    not null references questions (id) on delete cascade
);
insert into answers(id, name, is_correct, question_id)
values ('85948272-cebe-41eb-9ded-14d62bd9436f', 'Розовое вино', true, 'e5672044-90ca-4604-9c32-35037271f84e');
insert into answers(id, name, is_correct, question_id)
values ('cadd4fc1-2555-4a40-accd-b2f353297626', 'Минимал', false, 'e5672044-90ca-4604-9c32-35037271f84e');
insert into answers(id, name, is_correct, question_id)
values ('74a47b02-0c8d-47ec-97d7-565bb451e05c', 'Банк', true, '38727c7c-8dda-44f3-9f23-b7b9e1d307ec');
insert into answers(id, name, is_correct, question_id)
values ('8067b60f-f131-444c-8900-e65015417c99', 'Тамада', false, '38727c7c-8dda-44f3-9f23-b7b9e1d307ec');

insert into answers(id, name, is_correct, question_id)
values ('dd3a4cb3-f601-4ab6-a8fb-93f500e12ed4', 'Battlefield', false, '460d531b-2de3-4190-9d9d-99744e18acff');
insert into answers(id, name, is_correct, question_id)
values ('c4105279-fe9c-44e7-b917-6eb91c5f2b55', 'Call of Duty', true, '460d531b-2de3-4190-9d9d-99744e18acff');
insert into answers(id, name, is_correct, question_id)
values ('45133130-b536-4280-818b-74281c2fccb6', 'Assassins Creed 2', true, '648becaf-2f17-40c0-8a5f-b5e0eccae36b');
insert into answers(id, name, is_correct, question_id)
values ('b993e08a-98e0-4df9-86aa-6028ad7c3e61', 'Assassins Creed 3', false, '648becaf-2f17-40c0-8a5f-b5e0eccae36b');



create table if not exists quizzes
(
    id           uuid primary key default gen_random_uuid(),
    name         varchar     not null unique,
    topics       varchar     not null,
    difficulties varchar     not null,
    start_at      timestamptz not null
);

create table if not exists quizzes_to_questions
(
    quiz_id     uuid not null references quizzes(id) on delete cascade,
    question_id uuid not null references questions(id) on delete cascade,

    primary key (quiz_id, question_id)
);