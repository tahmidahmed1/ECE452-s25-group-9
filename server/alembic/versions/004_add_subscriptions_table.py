"""Add subscriptions table

Revision ID: 004_add_subscriptions_table
Revises: 003_add_badges_table
Create Date: 2025-01-18 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '004_add_subscriptions_table'
down_revision = '003_add_badges_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create user_subscriptions association table
    op.create_table('user_subscriptions',
    sa.Column('subscriber_id', sa.Integer(), nullable=False),
    sa.Column('organizer_id', sa.Integer(), nullable=False),
    sa.Column('subscribed_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['organizer_id'], ['users.id'], ondelete='CASCADE'),
    sa.ForeignKeyConstraint(['subscriber_id'], ['users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('subscriber_id', 'organizer_id')
    )


def downgrade() -> None:
    # Drop user_subscriptions table
    op.drop_table('user_subscriptions')
