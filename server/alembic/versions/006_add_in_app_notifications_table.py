"""add in app notifications table

Revision ID: 006
Revises: 005
Create Date: 2024-01-20 13:00:00.000000

"""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

# revision identifiers, used by Alembic.
revision = '006'
down_revision = '005'
branch_labels = None
depends_on = None

def upgrade():
    # Create in_app_notifications table
    op.create_table(
        'in_app_notifications',
        sa.Column('id', sa.Integer(), nullable=False),
        sa.Column('user_id', sa.Integer(), nullable=False),
        sa.Column('title', sa.String(), nullable=False),
        sa.Column('message', sa.Text(), nullable=False),
        sa.Column('data', sa.JSON(), nullable=True),
        sa.Column('is_read', sa.Boolean(), nullable=False, server_default='false'),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.text('now()'), nullable=True),
        sa.Column('read_at', sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(['user_id'], ['users.id'], ondelete='CASCADE'),
        sa.PrimaryKeyConstraint('id')
    )
    op.create_index(op.f('ix_in_app_notifications_id'), 'in_app_notifications', ['id'], unique=False)
    op.create_index('ix_in_app_notifications_user_id', 'in_app_notifications', ['user_id'], unique=False)
    op.create_index('ix_in_app_notifications_is_read', 'in_app_notifications', ['is_read'], unique=False)

def downgrade():
    # Drop in_app_notifications table
    op.drop_index('ix_in_app_notifications_is_read', table_name='in_app_notifications')
    op.drop_index('ix_in_app_notifications_user_id', table_name='in_app_notifications')
    op.drop_index(op.f('ix_in_app_notifications_id'), table_name='in_app_notifications')
    op.drop_table('in_app_notifications')
