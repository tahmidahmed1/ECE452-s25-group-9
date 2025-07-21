"""Add badges table

Revision ID: 003_add_badges_table
Revises: 002_add_messages_table
Create Date: 2025-01-17 10:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '003_add_badges_table'
down_revision = '002_add_messages_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create badges table
    op.create_table('badges',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('name', sa.String(), nullable=False),
    sa.Column('description', sa.String(), nullable=True),
    sa.Column('required_karma_points', sa.Integer(), nullable=False),
    sa.Column('icon_name', sa.String(), nullable=False),
    sa.Column('color', sa.String(), nullable=True),
    sa.Column('is_active', sa.Boolean(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_badges_id'), 'badges', ['id'], unique=False)
    
    # Create user_badges association table
    op.create_table('user_badges',
    sa.Column('user_id', sa.Integer(), nullable=False),
    sa.Column('badge_id', sa.Integer(), nullable=False),
    sa.Column('earned_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.ForeignKeyConstraint(['badge_id'], ['badges.id'], ),
    sa.ForeignKeyConstraint(['user_id'], ['users.id'], ),
    sa.PrimaryKeyConstraint('user_id', 'badge_id')
    )


def downgrade() -> None:
    # Drop user_badges table
    op.drop_table('user_badges')
    
    # Drop badges table
    op.drop_index(op.f('ix_badges_id'), table_name='badges')
    op.drop_table('badges')
