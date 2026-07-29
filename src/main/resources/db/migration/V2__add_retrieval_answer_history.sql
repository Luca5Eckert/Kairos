CREATE TABLE questions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_questions_user_id_created_at ON questions (user_id, created_at);

CREATE TABLE answers (
    id UUID PRIMARY KEY,
    question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    schema_version INTEGER NOT NULL,
    snapshot JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_answers_question_id_created_at ON answers (question_id, created_at);
