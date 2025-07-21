"""Add notifications table

Revision ID: 007_add_notifications_table
Revises: 006_add_event_registrations_table
Create Date: 2025-01-19 12:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '007_add_notifications_table'
down_revision = '006_add_event_registrations_table'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Create in_app_notifications table
    op.create_table('in_app_notifications',
    sa.Column('id', sa.Integer(), nullable=False),
    sa.Column('user_id', sa.Integer(), nullable=False),
    sa.Column('title', sa.String(), nullable=False),
    sa.Column('message', sa.Text(), nullable=False),
    sa.Column('notification_type', sa.String(), nullable=False),
    sa.Column('is_read', sa.Boolean(), nullable=False),
    sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
    sa.Column('data', sa.JSON(), nullable=True),
    sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
    sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_in_app_notifications_id'), 'in_app_notifications', ['id'], unique=False)


def downgrade() -> None:
    # Drop in_app_notifications table
    op.drop_index(op.f('ix_in_app_notifications_id'), table_name='in_app_notifications')
    op.drop_table('in_app_notifications')
