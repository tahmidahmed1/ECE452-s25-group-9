"""add_user_subscriptions_table

Revision ID: 002_add_user_subscriptions_table
Revises: f0c76622b24e
Create Date: 2025-01-16 12:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '002_add_user_subscriptions_table'
down_revision: Union[str, None] = 'f0c76622b24e'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Create user_subscriptions table"""
    op.create_table(
        'user_subscriptions',
        sa.Column('subscriber_id', sa.Integer(), nullable=False),
        sa.Column('organizer_id', sa.Integer(), nullable=False),
        sa.Column('subscribed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.ForeignKeyConstraint(['subscriber_id'], ['users.id'], ),
        sa.ForeignKeyConstraint(['organizer_id'], ['users.id'], ),
        sa.PrimaryKeyConstraint('subscriber_id', 'organizer_id')
    )


def downgrade() -> None:
    """Drop user_subscriptions table"""
    op.drop_table('user_subscriptions') 
